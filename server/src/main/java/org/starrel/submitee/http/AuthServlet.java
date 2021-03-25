package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.auth.AuthResult;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.model.UserRealm;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthServlet extends JsonServlet {

    @Override
    protected void request(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        if (!body.has("scheme") ||
                !body.has("realm") ||
                !body.has("content")) {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }

        String scheme = body.get("scheme").getAsString();
        String realm = body.get("realm").getAsString();

        UserRealm userRealm = SubmiteeServer.getInstance().getUserRealm(realm);
        if (userRealm == null) {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }

        AuthScheme authScheme = userRealm.getAuthScheme(scheme);
        if (authScheme == null) {
            resp.setStatus(HttpStatus.FORBIDDEN_403);
            return;
        }

        AuthResult result;
        try {
            result = authScheme.auth(body.get("content"));
        } catch (Exception exception) {
            resp.setStatus(500);
            ExceptionReporting.report("authenticating user, request body is "
                    + SubmiteeServer.GSON.toJson(body), exception);
            return;
        }

        resp.setStatus(200);
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
    }
}
