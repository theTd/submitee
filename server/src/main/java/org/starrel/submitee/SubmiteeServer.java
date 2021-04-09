package org.starrel.submitee;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SubmiteeServer implements SServer, AttributeHolder<SubmiteeServer> {
    public final static String ATTRIBUTE_KEY = "system";
    public final static Gson GSON = new Gson();

    private static SubmiteeServer instance;

    private final InetSocketAddress[] listenAddresses;

    private final MongoDatabase mongoDatabase;
    private final DataSource dataSource;
    private final Server jettyServer;
    private final Map<Class<?>, AttributeSerializer<?>> attributeSerializerMap = new HashMap<>();

    private final TextContainer textContainer;
    private final BlobStorageController blobStorageController;
    private final TemplateKeeper templateKeeper;
    private final ObjectMapController objectMapController;
    private final AnonymousUserRealm anonymousUserRealm;
    // endregion
    private final Logger logger;
    private AttributeMap<SubmiteeServer> attributeMap;
    // region settings
    private AttributeSpec<String> defaultLanguage;

    public SubmiteeServer(MongoDatabase mongoDatabase, DataSource dataSource, InetSocketAddress[] listenAddresses) throws IOException {
        instance = this;
        APIBridge.instance = this;

        this.listenAddresses = listenAddresses;
        this.logger = LoggerFactory.getLogger(SubmiteeServer.class);
        this.dataSource = dataSource;
        this.mongoDatabase = mongoDatabase;

        jettyServer = new Server();
        blobStorageController = new BlobStorageController(this);
        templateKeeper = new TemplateKeeper(this);
        textContainer = new TextContainer();
        objectMapController = new ObjectMapController(this);
        anonymousUserRealm = new AnonymousUserRealm();
    }

    private static Handler initServletHandler() {
        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);

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
        servletHandler.addServlet(ConfigurationServlet.class, "/configuration/*");
        return servletHandler;
    }

    private static Handler initStaticHandler() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        String staticDirectory = System.getenv("STATIC_DIRECTORY");
        if (staticDirectory == null || staticDirectory.isEmpty()) staticDirectory = "static";
        resourceHandler.setResourceBase(staticDirectory);
        ContextHandler contextHandler = new ContextHandler();
        contextHandler.setContextPath("/static");
        contextHandler.setHandler(resourceHandler);
        return contextHandler;
    }

    public static SubmiteeServer getInstance() {
        return instance;
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
                    response.setCharacterEncoding("utf-8");
                    response.getWriter().println(I18N.Http.INTERNAL_ERROR.format(request));
                    ExceptionReporting.report("error handler", "uncaught error occurred", stacktrace);
                }
                response.getWriter().close();
            }
        });
    }

    private void setupAttributeSerializers() {
        addAttributeSerializer(String.class, AttributeSerializers.STRING);
        addAttributeSerializer(Integer.class, AttributeSerializers.INTEGER);
        addAttributeSerializer(Double.class, AttributeSerializers.DOUBLE);
        addAttributeSerializer(Boolean.class, AttributeSerializers.BOOLEAN);
        addAttributeSerializer(Date.class, AttributeSerializers.DATE);
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

        setupAttributeSerializers();
        this.attributeMap = readAttributeMap(this, "management");

        // region setup attribute specs
        this.defaultLanguage = this.attributeMap.of("default-language", String.class);
        // endregion

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
            throw new IOException("failed initializing blob storage controller", e);
        }
        // endregion

        // region setup template keeper
        try {
            templateKeeper.init();
        } catch (Exception e) {
            throw new IOException("failed initializing template keeper", e);
        }
        // endregion

        // region setup object map
        try {
            objectMapController.init();
        } catch (SQLException e) {
            throw new IOException("failed initializing object map controller", e);
        }
        // endregion

        addUserRealm(new InternalAccountRealm(this));

        // region setup jetty connectors and start jetty server
        setupJettyConnectors(listenAddresses);
        setupJettyHandlers();
        jettyServer.start();
        // endregion
    }

    @Override
    public void addUserRealm(UserRealm userRealm) {

    }

    @Override
    public UserRealm getUserRealm(String name) {
        return null;
    }

    @Override
    public void addNotificationScheme(NotificationScheme notificationScheme) {

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
    public Blob getBlobById(int blobId) {
        return null;
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> createTemporaryAttributeMap(TContext context) {
        return new AttributeMapImpl<>(context, null);
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> createOrReadAttributeMap(TContext context, String collection) {
        return new AttributeMapImpl<>(context, collection);
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection) {
        AttributeMapImpl<TContext> map = new AttributeMapImpl<>(context, collection);
        map.readAttribute(getMongoDatabase());
        return map;
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
    public STemplateImpl getTemplate(String templateId) {
        return null;
    }

    @Override
    public STemplateImpl getTemplateFromUUID(UUID templateUUID) throws ExecutionException {
        return templateKeeper.getTemplate(templateUUID);
    }

    @Override
    public List<STemplateImpl> getTemplateAllVersion(String templateId) throws ExecutionException {
        return templateKeeper.getByQuery(Filters.eq("template-id", templateId));
    }

    @Override
    public List<String> getTemplateIds() {
        // TODO: 2021/4/4 implement
        throw new UnsupportedOperationException();
    }

    @Override
    public STemplateImpl getTemplateLatestVersion(String templateId) throws ExecutionException {
        return templateKeeper.getTemplateLatestVersion(templateId);
    }

    @Override
    public Submission getSubmission(UUID uniqueId) {
        // TODO: 2021/4/4 implement
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SubmissionImpl> getSubmissions(Bson query) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<UUID> getSubmissionIdsOfUser(UserDescriptor userDescriptor) {
        return null;
    }

    @Override
    public Submission createSubmission(UserDescriptor userDescriptor, STemplate template, JsonObject body) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public <TValue> AttributeSerializer<TValue> getAttributeSerializer(Class<TValue> type) {
        return (AttributeSerializer<TValue>) attributeSerializerMap.get(type);
    }

    @Override
    public Session getUserSession(String userRealmTypeId, String userId) {
        // TODO: 2021/3/23 query session from session database
        return null;
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
    public void reportException(String entity, String activity, String event) {
        // TODO: 2021/4/4
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportException(String entity, String activity, Throwable stacktrace) {
        // TODO: 2021/4/4
        throw new UnsupportedOperationException();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void shutdown() throws Exception {
        jettyServer.stop();
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
        // TODO: 2021-03-25-0025
        return null;
    }

    public User resumeSession(SessionImpl session) {
        // TODO: 2021-04-09-0009
        return null;
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
}
