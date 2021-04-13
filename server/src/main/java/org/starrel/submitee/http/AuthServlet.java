package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.AsyncContext;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.auth.AuthResult;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.model.STemplateImpl;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.UserRealm;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class AuthServlet extends AbstractJsonServlet {

    {
        setBaseUri("/auth");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length == 0) {
            ExceptionReporting.report(AuthServlet.class, "parsing uri", "unexpected uri: " + req.getRequestURI());
            responseBadRequest(req, resp);
            return;
        }

        List<UserRealm> realms = new ArrayList<>();

        switch (uriParts[0]) {
            case "template": {
                String uuidString = uriParts.length == 2 ? uriParts[1] : null;
                UUID uuid = null;
                try {
                    uuid = uuidString == null ? null : UUID.fromString(uuidString);
                } catch (Exception ignored) {
                }
                if (uuid == null) {
                    ExceptionReporting.report(AuthServlet.class, "parsing template uuid", "uri is " + req.getRequestURI());
                    responseBadRequest(req, resp);
                    return;
                }
                STemplateImpl template = null;
                try {
                    template = SubmiteeServer.getInstance().getTemplateFromUUID(uuid);
                } catch (ExecutionException e) {
                    ExceptionReporting.report(AuthServlet.class, "fetching template", e);
                }

                if (template == null) {
                    responseNotFound(req, resp);
                    return;
                }
                List<JsonObject> list = template.getAttributeMap().getList("protected.user-distinguish", JsonObject.class);
                for (JsonObject distinguish : list) {
                    String realm = distinguish.get("realm").getAsString();
                    realms.add(SubmiteeServer.getInstance().getUserRealm(realm));
                }
                if (template.getAttribute("allow-anonymous", Boolean.class, true)) {
                    realms.add(SubmiteeServer.getInstance().getAnonymousUserRealm());
                }
                break;
            }
            case "management": {
                realms.add(SubmiteeServer.getInstance().getInternalAccountRealm());
                break;
            }
            default: {
                ExceptionReporting.report(AuthServlet.class, "parsing method", "unknown method: " + uriParts[0]);
                responseBadRequest(req, resp);
                return;
            }
        }
        resp.setStatus(HttpStatus.OK_200);
        resp.setContentType("application/json");
        JsonWriter writer = new JsonWriter(resp.getWriter());
        writer.beginArray();
        for (UserRealm realm : realms) {
            writer.beginObject();
            writer.name("realm").value(realm.getTypeId());
            writer.name("title").value(I18N.fromKey(String.format("user_realm.%s.title", realm.getTypeId())).format(req));
            writer.name("scheme").beginArray();
            for (AuthScheme scheme : realm.getSupportedAuthSchemes()) {
                writer.beginObject();
                writer.name("name").value(scheme.getName());
                writer.name("url").value(scheme.getViewUrl(getSession(req)));
                Map<String, String> params = scheme.getParams(getSession(req));
                if (params != null) {
                    writer.name("params").jsonValue(SubmiteeServer.GSON.toJson(params));
                }
                writer.endObject();
            }
            writer.endArray().endObject();
        }
        writer.endArray();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        if (!body.has("scheme") ||
                !body.has("realm") ||
                !body.has("body")) {
            ExceptionReporting.report(AuthServlet.class, "parsing request body", "unexpected request body:" +
                    SubmiteeServer.GSON.toJson(body));
            responseBadRequest(req, resp);
            return;
        }

        String scheme = body.get("scheme").getAsString();
        String realm = body.get("realm").getAsString();

        UserRealm userRealm = SubmiteeServer.getInstance().getUserRealm(realm);
        if (userRealm == null) {
            responseAccessDenied(req, resp);
            return;
        }

        AuthScheme authScheme = userRealm.getAuthScheme(scheme);
        if (authScheme == null) {
            responseAccessDenied(req, resp);
            return;
        }

        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(() -> {
            try {
                AuthResult result;
                try {
                    result = authScheme.auth(getSession(req), body.get("body"));
                } catch (Exception exception) {
                    ExceptionReporting.report(AuthServlet.class, "authenticating user", exception);
                    responseInternalError(req, resp);
                    return;
                }

                resp.setStatus(HttpStatus.OK_200);
                resp.setContentType("application/json");

                JsonWriter writer = new JsonWriter(resp.getWriter());
                writer.beginObject();
                if (result.isAccepted()) {
                    writer.name("accepted").value(true);
                    getSession(req).setUser(result.getAcceptedUser());
                } else {
                    writer.name("accepted").value(false);
                    writer.name("deny-classify").value(result.getDenyClassify());
                    writer.name("deny-message").value(result.getDenyMessage());
                }

                if (result.getRedirect() != null) {
                    writer.name("redirect").value(result.getRedirect());
                }

                writer.endObject();
                writer.close();
            } catch (Exception e) {
                ExceptionReporting.report(AuthServlet.class, "processing authentication", e);
                try {
                    responseInternalError(req, resp);
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            } finally {
                asyncContext.complete();
            }
        });
    }
}
