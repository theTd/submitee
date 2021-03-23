package org.starrel.submitee.http;

import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.SessionImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

public class SubmiteeHttpServlet extends HttpServlet {

    private final SubmiteeServer submiteeServer;

    {
        submiteeServer = SubmiteeServer.getInstance();
    }

    @Override
    public void init() throws ServletException {
        if (submiteeServer == null)
            throw new ServletException(new NullPointerException("submitee server instance is null"));
    }

    public SubmiteeServer getSubmiteeServer() {
        return submiteeServer;
    }

    public Session getSession(HttpServletRequest request) {
        SessionImpl session = SessionImpl.getSession(request.getSession());
        if (session == null) {
            throw new RuntimeException("could not get session instance for servlet request");
        }
        return session;
    }
}
