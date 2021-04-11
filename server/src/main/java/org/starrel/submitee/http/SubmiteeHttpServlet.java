package org.starrel.submitee.http;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.SessionImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class SubmiteeHttpServlet extends HttpServlet {
    private static long errorPageCheck = -1;
    private static long errorPageLastModifies = -1;

    private static String errorPage;
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
        SessionImpl session = (SessionImpl) request.getSession().getAttribute(SessionImpl.HTTP_ATTRIBUTE_SESSION);
        if (session == null) {
            throw new RuntimeException("could not get session instance for servlet request");
        }
        return session;
    }

    @SneakyThrows
    public static String createErrorPage(String title) {
        // checks for error page file update per 30 seconds
        boolean checkUpdate = false;
        if (errorPageCheck == -1 || System.currentTimeMillis() - errorPageCheck > 1000 * 30) {
            checkUpdate = true;
            errorPageCheck = System.currentTimeMillis();
        }

        if (checkUpdate) {
            String staticDirectory = System.getenv("STATIC_DIRECTORY");
            if (staticDirectory == null || staticDirectory.isEmpty()) staticDirectory = "static";
            File errorPageFile = new File(staticDirectory + File.separator + "protected" + File.separator + "error-page.html");

            try {
                if (errorPageFile.lastModified() != errorPageLastModifies) {
                    InputStream stream = new FileInputStream(errorPageFile);
                    ByteArrayOutputStream buff = new ByteArrayOutputStream();
                    ByteStreams.copy(stream, buff);
                    errorPage = buff.toString(StandardCharsets.UTF_8);
                    errorPageLastModifies = errorPageFile.lastModified();
                    errorPageCache.invalidateAll();
                    SubmiteeServer.getInstance().getLogger().info("error page updated");
                }
            } catch (Exception e) {
                ExceptionReporting.report(SubmiteeHttpServlet.class, "reading error page", e);
                errorPage = "" +
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
                errorPageCache.invalidateAll();
            }
        }
        return errorPageCache.get(title, () -> errorPage.replaceAll("%ERROR_TITLE%", title));
    }

    public static void responseAccessDenied(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.Http.ACCESS_DENIED.format(req));
    }

    public static void responseBadRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        responseErrorPage(resp, HttpStatus.BAD_REQUEST_400, I18N.Http.INVALID_INPUT.format(req));
    }

    public static void responseBadRequest(HttpServletRequest req, HttpServletResponse resp, I18N.I18NKey messageKey, Object... messageParts) throws IOException {
        responseErrorPage(resp, HttpStatus.BAD_REQUEST_400, messageKey.format(req, messageParts));
    }

    public static void responseInternalError(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        responseErrorPage(resp, HttpStatus.INTERNAL_SERVER_ERROR_500, I18N.Http.INTERNAL_ERROR.format(req));
    }

    public static void responseNotFound(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        responseErrorPage(resp, HttpStatus.NOT_FOUND_404, I18N.Http.NOT_FOUND.format(req));
    }

    public static void responseErrorPage(HttpServletResponse resp, int statusCode, String errorTitle) throws IOException {
        resp.setHeader("SUBMITEE-ERROR-TITLE", URLEncoder.encode(errorTitle, StandardCharsets.UTF_8));
        resp.setStatus(statusCode);
        resp.setContentType("text/html");
        resp.getWriter().println(createErrorPage(errorTitle));
    }
}
