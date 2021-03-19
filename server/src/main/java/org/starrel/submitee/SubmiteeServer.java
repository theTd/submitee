package org.starrel.submitee;

import com.google.gson.Gson;
import com.mongodb.client.MongoDatabase;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.http.AuthServlet;
import org.starrel.submitee.http.SessionFilter;
import org.starrel.submitee.http.SubmitServlet;
import org.starrel.submitee.http.TemplateServlet;
import org.starrel.submitee.model.AuthScheme;
import org.starrel.submitee.model.NotificationScheme;
import org.starrel.submitee.model.UserRealm;

import javax.servlet.DispatcherType;
import java.net.InetSocketAddress;
import java.util.EnumSet;

public class SubmiteeServer implements SServer {
    public final static Gson GSON = new Gson();
    private final MongoDatabase mongoDatabase;
    private final Server jettyServer;

    public SubmiteeServer(MongoDatabase mongoDatabase, InetSocketAddress listenAddress) {
        this.mongoDatabase = mongoDatabase;

        jettyServer = new Server(listenAddress);
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.getServletContext().setAttribute("server", this);

        servletHandler.addServletWithMapping(SubmitServlet.class, "/submit");
        servletHandler.addServletWithMapping(TemplateServlet.class, "/template");
        servletHandler.addServletWithMapping(AuthServlet.class, "/auth");
        servletHandler.addFilterWithMapping(SessionFilter.class, "/", EnumSet.of(DispatcherType.REQUEST));

        jettyServer.setHandler(servletHandler);
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
    public void shutdown() throws Exception {
        jettyServer.stop();
    }

    public void join() throws Exception {
        jettyServer.join();
    }
}
