package org.starrel.submitee.http;

import org.starrel.submitee.SServer;
import org.starrel.submitee.model.SessionImpl;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class SessionFilter extends HttpFilter {
    private SServer server;

    @Override
    public void init() throws ServletException {
        this.server = (SServer) getServletContext().getAttribute("server");
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpSession httpSession = req.getSession();
        SessionImpl session = SessionImpl.getSession(httpSession);
        if (session == null) {
            SessionImpl.createAnonymous(server, httpSession);
        }
    }
}
