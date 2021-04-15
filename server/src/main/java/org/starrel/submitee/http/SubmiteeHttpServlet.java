package org.starrel.submitee.http;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.starrel.submitee.*;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.SessionImpl;
import org.starrel.submitee.model.SessionKeeper;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

public class SubmiteeHttpServlet extends HttpServlet {
    private static final Cache<String, String> errorPageCache = CacheBuilder.newBuilder().maximumSize(100).build();

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
        int idx = 0;
        while (idx < uri.length()) {
            int i = uri.indexOf('/', idx);
            if (i == -1) {
                String end = uri.substring(idx);
                if (!end.isEmpty()) list.add(end);
                break;
            } else if (i == idx) {
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
        SessionImpl session = (SessionImpl) request.getSession().getAttribute(SessionKeeper.HTTP_ATTRIBUTE_SESSION);
        if (session == null) {
            throw new RuntimeException("could not get session instance for servlet request");
        }
        return session;
    }

    private static String errorPageFilePath = null;
    private final static String DEFAULT_ERROR_PAGE = "" +
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\"\n" +
            "          content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\">\n" +
            "    <title>%ERROR_TITLE%</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<p>%ERROR_TITLE%</p>\n" +
            "</body>\n" +
            "</html>";

    @SneakyThrows
    public static String createErrorPage(String title) {
        if (errorPageFilePath == null) {

            errorPageFilePath = SubmiteeServer.getInstance().getStaticDirectory()
                    + File.separator + "protected" + File.separator + "error-page.html";
        }

        FileLoadingCache.Result result = SubmiteeServer.getInstance().getFileLoadingCache()
                .getFileContent(errorPageFilePath, "UTF-8", DEFAULT_ERROR_PAGE);

        if (result.getException() != null) {
            ExceptionReporting.report(SubmiteeHttpServlet.class, "reading error page", result.getException());
        }

        if (!result.isCached()) {
            errorPageCache.invalidateAll();
            SubmiteeServer.getInstance().getLogger().info("error page updated");
        }
        return errorPageCache.get(title, () -> result.getContent().replaceAll("%ERROR_TITLE%", title));
    }

    public static void responseAccessDenied(HttpServletRequest req, HttpServletResponse resp) {
        responseClassifiedError(req, resp, ClassifiedErrors.ACCESS_DENIED);
    }

    public static void responseBadRequest(HttpServletRequest req, HttpServletResponse resp) {
        responseClassifiedError(req, resp, ClassifiedErrors.BAD_REQUEST);
    }

    public static void responseInternalError(HttpServletRequest req, HttpServletResponse resp) {
        responseClassifiedError(req, resp, ClassifiedErrors.INTERNAL_SERVER_ERROR);
    }

    public static void responseNotFound(HttpServletRequest req, HttpServletResponse resp) {
        responseClassifiedError(req, resp, ClassifiedErrors.NOT_FOUND);
    }

    public static void responseClassifiedError(HttpServletRequest req, HttpServletResponse resp, ClassifiedError error, Object... messageParts) {
        responseErrorPage(resp, error.getHttpStatus(), error.getMessageKey().format(req, messageParts), error.getDistinguishName());
    }

    public static void responseClassifiedException(HttpServletRequest req, HttpServletResponse resp, ClassifiedException exception) {
        String title = null;
        try {
            title = exception.getClassifiedError().getMessageKey().format(req, exception.getMessageParts());
        } catch (Exception ignored) {
        }
        if (title == null) {
            title = exception.getMessage();
        }
        responseErrorPage(resp, exception.getClassifiedError().getHttpStatus(), title, exception.getDistinguishName());
    }

    public static void responseErrorPage(HttpServletResponse resp, int statusCode, String errorTitle, String errorClassify) {
        try {
            resp.setHeader("SUBMITEE-ERROR-TITLE", URLEncoder.encode(errorTitle, StandardCharsets.UTF_8));
            if (errorClassify != null) {
                resp.setHeader("SUBMITEE-ERROR-CLASSIFY", URLEncoder.encode(errorClassify, StandardCharsets.UTF_8));
            }
            resp.setStatus(statusCode);
            resp.setContentType("text/html");
            resp.getWriter().println(createErrorPage(errorTitle));
        } catch (Throwable e) {
            ExceptionReporting.report(SubmiteeHttpServlet.class, "sending error page", e);
        }
    }
}
