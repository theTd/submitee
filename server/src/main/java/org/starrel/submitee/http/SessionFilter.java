package org.starrel.submitee.http;

import org.slf4j.LoggerFactory;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.SessionImpl;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class SessionFilter extends HttpFilter {
    private final SubmiteeServer submiteeServer;

    {
        submiteeServer = SubmiteeServer.getInstance();
    }

    @Override
    public void init() throws ServletException {
        if (submiteeServer == null)
            throw new ServletException(new NullPointerException("submitee server instance is null"));
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpSession httpSession = req.getSession();
        SessionImpl session = SessionImpl.getSession(httpSession);
        if (session == null) {
            session = SessionImpl.fromCookie(req.getCookies());
        }
        if (session == null) {
            SessionImpl.createAnonymous(httpSession);
            LoggerFactory.getLogger(SessionFilter.class).warn("an anonymous session is created, http session id is " + req.getSession().getId());
        }
        chain.doFilter(req, res);
    }

    public SubmiteeServer getSubmiteeServer() {
        return submiteeServer;
    }
}
