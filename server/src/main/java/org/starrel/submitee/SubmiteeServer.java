package org.starrel.submitee;

import com.google.gson.Gson;
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
import org.starrel.submitee.attribute.*;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.auth.InternalAccountRealm;
import org.starrel.submitee.auth.PasswordAuthScheme;
import org.starrel.submitee.blob.BlobStorage;
import org.starrel.submitee.http.*;
import org.starrel.submitee.model.*;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Logger;

public class SubmiteeServer implements SServer, AttributeHolder<SubmiteeServer> {
    public final static Gson GSON = new Gson();

    private static SubmiteeServer instance;
    private final MongoDatabase mongoDatabase;
    private final DataSource dataSource;
    private final Server jettyServer;
    private final Map<Class<?>, AttributeSerializer<?>> attributeSerializerMap = new HashMap<>();

    private final AttributeMap<SubmiteeServer> attributeMap;
    private final AttributeSpec<Void> authSettingsSection;
    private final AttributeSpec<String> defaultLanguage;

    public SubmiteeServer(MongoDatabase mongoDatabase, DataSource dataSource, InetSocketAddress[] listenAddress) {
        this.dataSource = dataSource;
        instance = this;
        this.mongoDatabase = mongoDatabase;

        jettyServer = new Server();
        for (InetSocketAddress address : listenAddress) {
            ServerConnector connector = new ServerConnector(jettyServer);
            connector.setHost(address.getHostName());
            connector.setPort(address.getPort());
            jettyServer.addConnector(connector);
        }

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

        this.attributeMap = readAttributeMap(this, "management", getAttributePersistKey());
        this.authSettingsSection = this.attributeMap.of("auth-settings");
        this.defaultLanguage = this.attributeMap.of("default-language", String.class);
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

    @Override
    public void start() throws Exception {
        jettyServer.start();
        // TODO: 2021-03-17-0017  read exists templates
        // TODO: 2021-03-17-0017  initialize auth schemes
        // TODO: 2021-03-17-0017  initialize user realms
    }

    @Override
    public void addAuthScheme(AuthScheme authScheme) {

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
    public void addBlobStorage(BlobStorage blobStorage) {

    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context) {
        return null;
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context, String collection, String id) {
        return null;
    }

    @Override
    public <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection, String id) {
        return new AttributeMapImpl<>(context);
        // TODO: 2021/3/26 read from collection
    }

    @Override
    public <TValue> void addAttributeSerializer(Class<TValue> type, AttributeSerializer<TValue> serializer) {
        attributeSerializerMap.put(type, serializer);
    }

    @Override
    public User getUser(String realmType, String userId) {
        return null;
    }

    @Override
    public User getUser(UserDescriptor userDescriptor) {
        return null;
    }

    @Override
    public STemplateImpl createTemplate(String templateId) {
        return null;
    }

    @Override
    public STemplateImpl getTemplate(String templateId) {
        return null;
    }

    @Override
    public List<STemplateImpl> getTemplateAllVersion(String templateId) {
        return null;
    }

    @Override
    public List<String> getTemplateIds() {
        return null;
    }

    @Override
    public int getTemplateLatestVersion(String templateId) {
        return 0;
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
        // TODO: 2021/3/23
        return null;
    }

    @Override
    public I18N.I18NKey getI18nKey(String key) {
        return null;
    }

    @Override
    public void reportException(Throwable throwable) {

    }

    @Override
    public void reportException(String activity, Throwable throwable) {

    }

    @Override
    public Logger getLogger() {
        return null;
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
        return "server";
    }

    @Override
    public AttributeMap<? extends SubmiteeServer> getAttributeMap() {
        return attributeMap;
    }

    public AttributeSpec<Void> getAuthSettingsSection() {
        return authSettingsSection;
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

    public Object getObjectFromUUID(UUID uniqueId) {
        // TODO: 2021/3/26
        return null;
    }
}
