package org.starrel.submitee.http;

import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.SessionImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

        SessionImpl session = (SessionImpl) httpSession.getAttribute(SessionImpl.HTTP_ATTRIBUTE_SESSION);
        if (session == null) {
            SessionImpl.createFromHttpRequest(req);
        }

        chain.doFilter(req, res);
    }

    public SubmiteeServer getSubmiteeServer() {
        return submiteeServer;
    }
}
