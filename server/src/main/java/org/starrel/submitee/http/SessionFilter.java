package org.starrel.submitee.http;

import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.SessionImpl;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
        HttpSession httpSession = req.getSession();
        SessionImpl session = SessionImpl.getSession(httpSession);
        if (session == null) {
            session = SessionImpl.fromCookie(req.getCookies());
        }
        if (session == null) {
            SessionImpl.createAnonymous(SubmiteeServer.getInstance(), httpSession);
        }
    }

    public SubmiteeServer getSubmiteeServer() {
        return submiteeServer;
    }
}
