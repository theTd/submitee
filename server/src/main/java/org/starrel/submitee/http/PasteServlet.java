package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.STemplateImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PasteServlet extends AbstractJsonServlet {
    @Override
    protected void request(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        if (!body.has("target") ||
                !body.get("target").isJsonPrimitive() ||
                !body.has("content") ||
                !body.get("content").isJsonObject()) {
            ExceptionReporting.report(PasteServlet.class, "parsing request body",
                    "unexpected request body: " + SubmiteeServer.GSON.toJson(body));
            responseBadRequest(req, resp);
            return;
        }

        String target = body.get("target").getAsString();
        UUID uuid;
        try {
            uuid = UUID.fromString(target);
        } catch (Exception e) {
            ExceptionReporting.report(PasteServlet.class, "parsing uuid", e);
            responseBadRequest(req, resp);
            return;
        }

        JsonObject content = body.get("content").getAsJsonObject();
        String type;
        try {
            type = SubmiteeServer.getInstance().getObjectMapController().getType(uuid);
        } catch (ExecutionException e) {
            ExceptionReporting.report(PasteServlet.class, "determining object type from uuid", e);
            responseBadRequest(req, resp);
            return;
        }

        switch (type) {
            case "template": {
                try {
                    STemplateImpl template = SubmiteeServer.getInstance().getTemplateKeeper().getTemplate(uuid);
                    template.getAttributeMap().set("", content);
                    // TODO: 2021-04-06-0006 switch to apply method
                    resp.setStatus(HttpStatus.OK_200);
                } catch (Exception e) {
                    ExceptionReporting.report(PasteServlet.class, "applying content", e);
                    responseInternalError(req, resp);
                }
                break;
            }
            case "submission": {
                // TODO: 2021-04-06-0006
                throw new UnsupportedOperationException();
            }
            default: {
                ExceptionReporting.report(PasteServlet.class, "unknown type", "unknown type: " + type + ", uuid=" + uuid);
                responseInternalError(req, resp);
            }
        }
    }
}
