package org.starrel.submitee.http;

import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.SessionImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SubmiteeHttpServlet extends HttpServlet {

    private final SubmiteeServer submiteeServer;
    private String baseUri = "";

    {
        submiteeServer = SubmiteeServer.getInstance();
    }

    protected void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String[] parseUri(String uri) {
        uri = uri.substring(uri.indexOf(baseUri) + baseUri.length());

        List<String> list = new LinkedList<>();
        int idx = 1;
        while (true) {
            int i = uri.indexOf('/', idx);
            if (i == -1) {
                list.add(uri.substring(idx));
                break;
            } else if (i == idx) {
                list.add("");
                idx++;
            } else {
                list.add(uri.substring(idx, i));
                idx = i + 1;
            }
        }
        return list.toArray(new String[0]);
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
        SessionImpl session = (SessionImpl) request.getSession().getAttribute(SessionImpl.HTTP_ATTRIBUTE_SESSION);
        if (session == null) {
            throw new RuntimeException("could not get session instance for servlet request");
        }
        return session;
    }

    protected void responseBadRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpStatus.BAD_REQUEST_400);
        resp.getWriter().println(I18N.Http.INVALID_INPUT.format(req));
    }

    protected void responseBadRequest(HttpServletRequest req, HttpServletResponse resp, I18N.I18NKey messageKey, Object... messageParts) throws IOException {
        resp.setStatus(HttpStatus.BAD_REQUEST_400);
        resp.getWriter().println(messageKey.format(req, messageParts));
    }

    protected void responseInternalError(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        resp.getWriter().println(I18N.Http.INTERNAL_ERROR.format(req));
    }

    protected void responseNotFound(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpStatus.NOT_FOUND_404);
        resp.getWriter().println(I18N.Http.NOT_FOUND.format(req));
    }
}
