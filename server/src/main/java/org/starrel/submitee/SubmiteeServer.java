package org.starrel.submitee;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starrel.submitee.attribute.*;
import org.starrel.submitee.auth.AnonymousUserRealm;
import org.starrel.submitee.auth.InternalAccountRealm;
import org.starrel.submitee.auth.PasswordAuthScheme;
import org.starrel.submitee.auth.PasswordAuthSchemeImpl;
import org.starrel.submitee.blob.Blob;
import org.starrel.submitee.blob.BlobStorageController;
import org.starrel.submitee.blob.BlobStorageProvider;
import org.starrel.submitee.blob.FileTreeBlobStorage;
import org.starrel.submitee.http.*;
import org.starrel.submitee.language.TextContainer;
import org.starrel.submitee.model.*;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class SubmiteeServer implements SServer, AttributeHolder<SubmiteeServer> {
    public final static String ATTRIBUTE_KEY = "system";
    public final static Gson GSON = new Gson();

    public final static JsonWriterSettings JSON_WRITER_SETTINGS = JsonWriterSettings.builder()
            .int64Converter((value, writer) -> writer.writeNumber(value + ""))
            .build();

    private static SubmiteeServer instance;

    private final InetSocketAddress[] listenAddresses;

    private final FileLoadingCache fileLoadingCache = new FileLoadingCache(30 * 1000);

    private final MongoDatabase mongoDatabase;
    private final DataSource dataSource;
    private final Server jettyServer;
    private final Map<Class<?>, AttributeSerializer<?>> attributeSerializerMap = Maps.newConcurrentMap();

    private final TextContainer textContainer;
    private final BlobStorageController blobStorageController;
    private final TemplateKeeper templateKeeper;
    private final ObjectMapController objectMapController;
    private final AnonymousUserRealm anonymousUserRealm;
    private final Map<String, UserRealm> userRealmMap = Maps.newConcurrentMap();
    private InternalAccountRealm internalAccountRealm;

    private final Map<String, NotificationScheme> notificationSchemeMap = Maps.newConcurrentMap();

    private final SessionKeeper sessionKeeper = new SessionKeeper();
    private final SubmissionKeeper submissionKeeper = new SubmissionKeeper();

    private final EventLogService eventLogService = new EventLogService();

    // endregion
    private final Logger logger;
    private final AttributeMap<SubmiteeServer> attributeMap;
    // region settings
    private final AttributeSpec<String> defaultLanguage;
    private ServletContextHandler servletHandler;

    public SubmiteeServer(MongoDatabase mongoDatabase, DataSource dataSource, InetSocketAddress[] listenAddresses) throws Exception {
        instance = this;
        APIBridge.instance = this;

        this.listenAddresses = listenAddresses;
        this.logger = LoggerFactory.getLogger("SubmiteeServer");
        this.dataSource = dataSource;
        this.mongoDatabase = mongoDatabase;

        jettyServer = new Server();
        blobStorageController = new BlobStorageController(this);
        templateKeeper = new TemplateKeeper(this);
        textContainer = new TextContainer();
        objectMapController = new ObjectMapController(this);
        anonymousUserRealm = new AnonymousUserRealm();

        setupAttributeSerializers();
        this.attributeMap = readAttributeMap(this, "management");

        // region setup attribute specs
        this.defaultLanguage = this.attributeMap.of("default-language", String.class);
        // endregion
    }

    private static Handler initStaticHandler() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase(getStaticDirectory());
        ContextHandler contextHandler = new ContextHandler();
        contextHandler.setContextPath("/static/");
        contextHandler.setProtectedTargets(new String[]{"/protected"});
        contextHandler.setHandler(resourceHandler);
        contextHandler.addAliasCheck((path, resource) -> true);
        return contextHandler;
    }

    public static SubmiteeServer getInstance() {
        return instance;
    }

    private Handler initServletHandler() {
        servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);

//        servletHandler.addFilter(UncaughtExceptionFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
        servletHandler.addFilter(ConnectionThrottleFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        servletHandler.addFilter(CharsetFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        servletHandler.addFilter(SessionFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        servletHandler.addServlet(AuthServlet.class, "/auth/*");
        servletHandler.addServlet(CreateServlet.class, "/create/*");
        servletHandler.addServlet(PasteServlet.class, "/paste/*");
        servletHandler.addServlet(InfoServlet.class, "/info/*");
        servletHandler.addServlet(BatchGetServlet.class, "/batch-get/*");
        servletHandler.addServlet(UploadServlet.class, "/upload/*");
        servletHandler.addServlet(GetFileServlet.class, "/get-file/*");
        servletHandler.addServlet(ConfigurationServlet.class, "/configuration/*");
        servletHandler.addServlet(SessionServlet.class, "/session/*");
        servletHandler.addServlet(TemplateControlServlet.class, "/template-control/*");
        servletHandler.addServlet(EventsServlet.class, "/events/*");
        return servletHandler;
    }

    public ServletContextHandler getServletHandler() {
        return servletHandler;
    }

    private void setupJettyConnectors(InetSocketAddress[] listenAddresses) {
        for (InetSocketAddress address : listenAddresses) {
            ServerConnector connector = new ServerConnector(jettyServer);
            connector.setHost(address.getHostName());
            connector.setPort(address.getPort());
            jettyServer.addConnector(connector);
        }
    }

    private void setupJettyHandlers() {
        DefaultSessionIdManager sessionIdManager = new DefaultSessionIdManager(jettyServer);
        sessionIdManager.setWorkerName("def");
        jettyServer.setSessionIdManager(sessionIdManager);

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(initStaticHandler());
        handlerList.addHandler(initServletHandler());
        jettyServer.setHandler(handlerList);
        jettyServer.setErrorHandler(new ErrorHandler() {
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
                Throwable stacktrace = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
                if (stacktrace != null) {
                    ExceptionReporting.report("error handler", "uncaught error occurred", stacktrace);
                }

                String message = null;
                switch (response.getStatus()) {
                    case 403:
                    case 404: {
                        message = I18N.Http.NOT_FOUND.format(request);
                        break;
                    }
                    case 500: {
                        message = I18N.Http.INTERNAL_ERROR.format(request);
                        break;
                    }
                }

                SubmiteeHttpServlet.responseErrorPage(response, response.getStatus(),
                        message == null ? I18N.Http.UNKNOWN_ERROR.format(request) : message, "UNKNOWN_ERROR");
            }
        });
    }

    private void setupAttributeSerializers() {
        addAttributeSerializer(String.class, AttributeSerializers.STRING);
        addAttributeSerializer(Integer.class, AttributeSerializers.INTEGER);
        addAttributeSerializer(Long.class, AttributeSerializers.LONG);
        addAttributeSerializer(Double.class, AttributeSerializers.DOUBLE);
        addAttributeSerializer(Boolean.class, AttributeSerializers.BOOLEAN);
        addAttributeSerializer(Date.class, AttributeSerializers.DATE);
        addAttributeSerializer(UUID.class, AttributeSerializers.UUID);
        addAttributeSerializer(UserDescriptor.class, UserDescriptor.SERIALIZER);
        addAttributeSerializer(HistoryAddressEntry.class, HistoryAddressEntry.SERIALIZER);

        addAttributeSerializer(JsonObject.class, new AttributeSerializer<JsonObject>() {
            @Override
            public JsonObject parse(JsonElement json) {
                return json.getAsJsonObject();
            }

            @Override
            public JsonElement write(JsonObject jsonObject) {
                return jsonObject;
            }
        });
        addAttributeSerializer(JsonArray.class, new AttributeSerializer<JsonArray>() {
            @Override
            public JsonArray parse(JsonElement json) {
                return json.getAsJsonArray();
            }

            @Override
            public JsonElement write(JsonArray jsonElements) {
                return jsonElements;
            }
        });
    }

    @Override
    public void start() throws Exception {

        // region setup language
        if (this.defaultLanguage.get() == null) {
            this.defaultLanguage.set("zh-CN");
        }
        textContainer.init();
        // endregion

        // region setup blob storage
        addBlobStorageProvider(FileTreeBlobStorage.PROVIDER);

        try {
            blobStorageController.init();
        } catch (SQLException e) {
            throw new Exception("failed initializing blob storage controller", e);
        }
        // endregion

        // region setup template keeper
        try {
            templateKeeper.init();
        } catch (Exception e) {
            throw new Exception("failed initializing template keeper", e);
        }
        // endregion

        // region setup submission keeper
        try {
            submissionKeeper.init();
        } catch (Exception e) {
            throw new Exception("failed initializing submission keeper", e);
        }
        // endregion

        // region setup object map
        try {
            objectMapController.init();
        } catch (SQLException e) {
            throw new Exception("failed initializing object map controller", e);
        }
        // endregion

        // region event log
        try {
            eventLogService.init();
        } catch (Exception e) {
            throw new Exception("failed initializing event log service", e);
        }
        // endregion

        // region setup jetty connectors and start jetty server
        setupJettyHandlers();
        setupJettyConnectors(listenAddresses);
        jettyServer.start();
        // endregion

        internalAccountRealm = new InternalAccountRealm();
        internalAccountRealm.init();
        addUserRealm(internalAccountRealm);

        addNotificationScheme(new EmailNotificationScheme());
        // TODO: 2021-04-13-0013 other initializations
    }

    @Override
    public void addUserRealm(UserRealm userRealm) {
        if (userRealmMap.containsKey(userRealm.getTypeId()))
            throw new RuntimeException("user realm type id conflict: " + userRealm.getTypeId());

        userRealmMap.put(userRealm.getTypeId(), userRealm);
    }

    @Override
    public UserRealm getUserRealm(String name) {
        return userRealmMap.get(name);
    }

    @Override
    public void addNotificationScheme(NotificationScheme notificationScheme) {
        if (notificationSchemeMap.containsKey(notificationScheme.getTypeId())) {
            throw new RuntimeException("notification scheme type id conflicts: " + notificationScheme.getTypeId());
        }
        notificationSchemeMap.put(notificationScheme.getTypeId(), notificationScheme);
    }

    @Override
    public void addBlobStorageProvider(BlobStorageProvider provider) {
        blobStorageController.addProvider(provider);
    }

    @Override
    public Blob createBlob(String blobStorageName, String fileName, String contentType, UserDescriptor uploader) throws IOException, SQLException {
        return blobStorageController.createNewBlob(blobStorageName, fileName, contentType, uploader);
    }

    @Override
    public Blob getBlobByKey(String blobKey) throws Exception {
        return blobStorageController.getBlobByKey(blobKey);
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> createTemporaryAttributeMap(TContext context) {
        return new AttributeMapImpl<>(context, null);
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> accessAttributeMap(TContext context, String collection) {
        return new AttributeMapImpl<>(context, collection);
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection) {
        AttributeMapImpl<TContext> map = new AttributeMapImpl<>(context, collection);
        map.read();
        return map;
    }

    @Override
    public boolean attributeMapExist(String persistId, String collection) {
        Document found = mongoDatabase.getCollection(collection).find(Filters.eq("id", persistId)).first();
        return found != null;
    }

    @Override
    public <TValue> void addAttributeSerializer(Class<TValue> type, AttributeSerializer<TValue> serializer) {
        attributeSerializerMap.put(type, serializer);
    }

    @Override
    public User getUser(String realmType, String userId) {
        UserRealm userRealm = getUserRealm(realmType);
        if (userRealm == null) return null;
        return userRealm.getUser(userId);
    }

    @Override
    public User getUser(UserDescriptor userDescriptor) {
        return getUser(userDescriptor.getRealmType(), userDescriptor.getUserId());
    }

    @Override
    public STemplateImpl createTemplate() throws Exception {
        return createTemplate(TemplateKeeper.DEFAULT_GROUPING);
    }

    @Override
    public STemplateImpl createTemplate(String grouping) throws Exception {
        return templateKeeper.createNewTemplate(grouping);
    }

    @Override
    public STemplateImpl getTemplate(UUID templateUniqueId) throws ExecutionException {
        return templateKeeper.getTemplate(templateUniqueId);
    }

    @Override
    public List<STemplateImpl> getTemplateAllVersion(String templateId) throws ExecutionException {
        return templateKeeper.getByQuery(Filters.eq("template-id", templateId));
    }

    @Override
    public Set<String> getTemplateIds() {
        return templateKeeper.getIds();
    }

    @Override
    public STemplateImpl getTemplateLatestVersion(String templateId) throws ExecutionException {
        return templateKeeper.getTemplateLatestVersion(templateId);
    }

    @Override
    public Submission getSubmission(UUID uniqueId) throws ExecutionException {
        return submissionKeeper.getSubmission(uniqueId);
    }

    @Override
    public List<SubmissionImpl> getSubmissions(Bson filter) throws ExecutionException {
        return submissionKeeper.getSubmissions(filter);
    }

    @Override
    public List<UUID> getSubmissionIdsOfUser(UserDescriptor userDescriptor) {
        return submissionKeeper.getSubmissionUUIDs(Filters.eq("body.submit-user"));
    }

    @Override
    public Submission createSubmission(UserDescriptor userDescriptor, STemplate template) {
        return submissionKeeper.create(userDescriptor, template);
    }

    @SuppressWarnings("unchecked")
    public <TValue> AttributeSerializer<TValue> getAttributeSerializer(Class<TValue> type) {
        return (AttributeSerializer<TValue>) attributeSerializerMap.get(type);
    }

    @Override
    public Session getUserSession(UserDescriptor userDescriptor) throws ExecutionException {
        return sessionKeeper.getByUser(userDescriptor);
    }

    @Override
    public PasswordAuthScheme createPasswordAuthScheme() {
        return new PasswordAuthSchemeImpl();
    }

    @Override
    public I18N.I18NKey getI18nKey(String key) {
        return textContainer.get(key);
    }

    @Override
    public void pushEvent(Level level, String entity, String activity, String detail) {
        eventLogService.pushEvent(level, entity, activity, detail);
    }

    @Override
    public void pushEvent(Level level, String entity, String activity, Throwable stacktrace) {
        eventLogService.pushEvent(level, entity, activity, stringifyThrowable(stacktrace));
    }

    @Override
    public void pushEvent(Level level, String entity, String activity, String detail, Throwable stacktrace) {
        eventLogService.pushEvent(level, entity, activity, detail + System.lineSeparator() + stringifyThrowable(stacktrace));
    }

    private String stringifyThrowable(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public List<UserRealm> getUserRealms() {
        return new ArrayList<>(userRealmMap.values());
    }

    @Override
    public void shutdown() throws Exception {
        jettyServer.stop();
        eventLogService.shutdown();
    }

    public void join() throws Exception {
        jettyServer.join();
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getAttributePersistKey() {
        return ATTRIBUTE_KEY;
    }

    @Override
    public AttributeMap<? extends SubmiteeServer> getAttributeMap() {
        return attributeMap;
    }

    public void removeAttributeMap(String collectionName, String attributePersistKey) {
        mongoDatabase.getCollection(collectionName).deleteMany(Filters.eq("id", attributePersistKey));
    }

    public String getDefaultLanguage() {
        return defaultLanguage.get();
    }

    public void setDefaultLanguage(String language) {
        this.defaultLanguage.set(language);
    }

    @Override
    public void attributeUpdated(String path) {
        // TODO: 2021-03-25-0025
    }

    public InternalAccountRealm getInternalAccountRealm() {
        return internalAccountRealm;
    }

    public User resumeSession(SessionImpl session) {
        for (UserRealm r : userRealmMap.values()) {
            if (r instanceof AnonymousUserRealm) continue;
            User resumed = r.resumeSession(session);
            if (resumed != null) return resumed;
        }
        return anonymousUserRealm.resumeSession(session);
    }

    public TemplateKeeper getTemplateKeeper() {
        return templateKeeper;
    }

    public ObjectMapController getObjectMapController() {
        return objectMapController;
    }

    public AnonymousUserRealm getAnonymousUserRealm() {
        return anonymousUserRealm;
    }

    public BlobStorageController getBlobStorageController() {
        return blobStorageController;
    }

    public FileLoadingCache getFileLoadingCache() {
        return fileLoadingCache;
    }

    public static String getStaticDirectory() {
        String staticDirectory = System.getenv("STATIC_DIRECTORY");
        if (staticDirectory == null) staticDirectory = "static" + File.separator;
        if (!staticDirectory.endsWith(File.separator)) staticDirectory += File.separator;
        return staticDirectory;
    }

    public SessionKeeper getSessionKeeper() {
        return sessionKeeper;
    }

    public EventLogService getEventLogService() {
        return eventLogService;
    }
}
