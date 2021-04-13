package org.starrel.submitee.http;

import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.SessionImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.starrel.submitee.model.SessionKeeper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class SessionFilter extends HttpFilter {
    private final SubmiteeServer submiteeServer;
    private final Logger logger;
    private final DateFormat dateFormat = new SimpleDateFormat("MM-dd_HH:mm:ss");

    {
        submiteeServer = SubmiteeServer.getInstance();
        logger = submiteeServer.getLogger();
    }

    @Override
    public void init() throws ServletException {
        if (submiteeServer == null)
            throw new ServletException(new NullPointerException("submitee server instance is null"));
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpSession httpSession = req.getSession();

        SessionImpl session = (SessionImpl) httpSession.getAttribute(SessionKeeper.HTTP_ATTRIBUTE_SESSION);
        if (session == null) {
            session = SubmiteeServer.getInstance().getSessionKeeper().fromHttpRequest(req);
            Cookie cookie = new Cookie(SessionKeeper.COOKIE_NAME_SESSION_TOKEN, session.getSessionToken());
            res.addCookie(cookie);
        }

        long start = System.currentTimeMillis();
        chain.doFilter(req, res);
        long end = System.currentTimeMillis();
        logger.info(String.format("%s HTTP %s %s FROM %s COSTS %dms USER %s",
                dateFormat.format(start), req.getMethod(), req.getRequestURI(),
                req.getAttribute("REMOTE_ADDR"), end - start,
                session.getUser().getDescriptor().toString()));
    }

    public SubmiteeServer getSubmiteeServer() {
        return submiteeServer;
    }
}
