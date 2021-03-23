package org.starrel.submitee;

import com.google.gson.Gson;
import com.mongodb.client.MongoDatabase;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSerializer;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.auth.PasswordAuthScheme;
import org.starrel.submitee.blob.BlobStorage;
import org.starrel.submitee.http.AuthServlet;
import org.starrel.submitee.http.SessionFilter;
import org.starrel.submitee.http.SubmitServlet;
import org.starrel.submitee.http.TemplateServlet;
import org.starrel.submitee.model.NotificationScheme;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.UserRealm;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SubmiteeServer implements SServer {
    public final static Gson GSON = new Gson();

    private static SubmiteeServer instance;
    private final MongoDatabase mongoDatabase;
    private final DataSource dataSource;
    private final Server jettyServer;
    private final Map<Class<?>, AttributeSerializer<?>> attributeSerializerMap = new HashMap<>();

    public SubmiteeServer(MongoDatabase mongoDatabase, DataSource dataSource, InetSocketAddress listenAddress) {
        this.dataSource = dataSource;
        instance = this;
        this.mongoDatabase = mongoDatabase;

        jettyServer = new Server(listenAddress);
        DefaultSessionIdManager sessionIdManager = new DefaultSessionIdManager(jettyServer);
        sessionIdManager.setWorkerName("default");
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

    private static Handler initServletHandler() {
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(SubmitServlet.class, "/submit");
        servletHandler.addServletWithMapping(TemplateServlet.class, "/template");
        servletHandler.addServletWithMapping(AuthServlet.class, "/auth");
        servletHandler.addFilterWithMapping(SessionFilter.class, "/", EnumSet.of(DispatcherType.REQUEST));
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
        return null;
    }

    @Override
    public <TValue> void addAttributeSerializer(Class<TValue> type, AttributeSerializer<TValue> serializer) {
        attributeSerializerMap.put(type, serializer);
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
}
