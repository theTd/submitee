package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SessionServlet extends AbstractJsonServlet {

    {
        setBaseUri("/session");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Session session = getSession(req);
        User user = session.getUser();

        Map<String, Object> info = new LinkedHashMap<>();
        if (user != null) {
            info.put("realm", user.getTypeId());
            info.put("id", user.getId());
            info.put("profile", user.getAttributeMap().of("profile").toJsonTree());
            info.put("grecaptcha-sitekey", SubmiteeServer.getInstance().getAttribute("grecaptcha-sitekey", String.class));
        }
        resp.setStatus(HttpStatus.OK_200);
        resp.setContentType("application/json");
        resp.getWriter().println(SubmiteeServer.GSON.toJson(info));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length < 1) {
            responseBadRequest(req, resp);
            return;
        }

        switch (uriParts[0]) {
            case "close": {
                Session session = getSession(req);
                if (session != null) session.close();
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            default: {
                ExceptionReporting.report(SessionServlet.class, "parsing method", "unknown method: " + uriParts[0]);
                responseBadRequest(req, resp);
                return;
            }
        }
    }
}
