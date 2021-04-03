package org.starrel.submitee;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoDatabase;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starrel.submitee.attribute.*;
import org.starrel.submitee.auth.InternalAccountRealm;
import org.starrel.submitee.auth.PasswordAuthScheme;
import org.starrel.submitee.auth.PasswordAuthSchemeImpl;
import org.starrel.submitee.blob.BlobStorageController;
import org.starrel.submitee.blob.BlobStorageProvider;
import org.starrel.submitee.blob.FileTreeBlobStorage;
import org.starrel.submitee.http.*;
import org.starrel.submitee.language.TextContainer;
import org.starrel.submitee.model.*;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SubmiteeServer implements SServer, AttributeHolder<SubmiteeServer> {
    public final static String ATTRIBUTE_KEY = "system";
    public final static Gson GSON = new Gson();

    private static SubmiteeServer instance;
    private final MongoDatabase mongoDatabase;
    private final DataSource dataSource;
    private final Server jettyServer;
    private final BlobStorageController blobStorageController = new BlobStorageController(this);
    private final TemplateKeeper templateKeeper = new TemplateKeeper(this);

    private final Map<Class<?>, AttributeSerializer<?>> attributeSerializerMap = new HashMap<>();
    private final AttributeMap<SubmiteeServer> attributeMap;

    private final TextContainer textContainer;

    // region settings
    private final AttributeSpec<String> defaultLanguage;
    // endregion

    private final Logger logger;

    public SubmiteeServer(MongoDatabase mongoDatabase, DataSource dataSource, InetSocketAddress[] listenAddresses) throws IOException {
        instance = this;
        APIBridge.instance = this;

        this.logger = LoggerFactory.getLogger(SubmiteeServer.class);
        this.dataSource = dataSource;
        this.mongoDatabase = mongoDatabase;

        jettyServer = new Server();
        setupJettyConnectors(listenAddresses);
        setupJettyHandlers();

        setupAttributeSerializers();
        this.attributeMap = readAttributeMap(this, "management");

        // region setup attribute specs
        this.defaultLanguage = this.attributeMap.of("default-language", String.class);
        // endregion

        // region setup language
        if (this.defaultLanguage.get() == null) {
            this.defaultLanguage.set("zh-CN");
        }
        textContainer = new TextContainer();
        textContainer.init();
        try {
            Class.forName("org.starrel.submitee.I18N");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
        textContainer.updateTemplate(new File("text" + File.separator + "template.properties"), I18N.ConstantI18NKey.KNOWN_KEYS);
        // endregion
    }

    private static Handler initServletHandler() {
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addFilterWithMapping(ConnectionThrottleFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        servletHandler.addFilterWithMapping(SessionFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        servletHandler.addServletWithMapping(AuthServlet.class, "/auth/*");
        servletHandler.addServletWithMapping(CreateServlet.class, "/create/*");
        servletHandler.addServletWithMapping(PasteServlet.class, "/paste/*");
        servletHandler.addServletWithMapping(InfoServlet.class, "/info/*");
        return servletHandler;
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

        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.addHandler(initServletHandler());

        SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setHandler(handlerCollection);

        jettyServer.setHandler(sessionHandler);
        jettyServer.setErrorHandler(new ErrorHandler() {
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.getWriter().close();
            }
        });
    }

    private void setupAttributeSerializers() {
        addAttributeSerializer(String.class, AttributeSerializers.STRING);
        addAttributeSerializer(Integer.class, AttributeSerializers.INTEGER);
        addAttributeSerializer(Double.class, AttributeSerializers.DOUBLE);
        addAttributeSerializer(Boolean.class, AttributeSerializers.BOOLEAN);
        addAttributeSerializer(UserDescriptor.class, AttributeSerializers.USER_DESCRIPTOR);
    }

    @Override
    public void start() throws Exception {
        jettyServer.start();
        // region setup blob storage
        addBlobStorageProvider(FileTreeBlobStorage.PROVIDER);

        try {
            blobStorageController.init();
        } catch (SQLException e) {
            throw new IOException("failed initializing blob storage controller", e);
        }
        // endregion

        // region init template keeper
        try {
            templateKeeper.init();
        } catch (Exception e) {
            throw new IOException("failed initializing template keeper", e);
        }
        // endregion
        addUserRealm(new InternalAccountRealm(this));
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
        // TODO: 2021-03-30-0030
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context) {
        return new AttributeMapImpl<>(context, null);
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context, String collection) {
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
    public List<STemplateImpl> getTemplateAllVersion(String templateId) {
        return templateKeeper.getTemplateAllVersions(templateId);
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

    public String getDefaultLanguage() {
        return defaultLanguage.get();
    }

    public void setDefaultLanguage(String language) throws AttributeFilter.FilterException {
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

    public Object getObjectFromUUID(UUID uniqueId) {
        // TODO: 2021/3/26
        return null;
    }
}
