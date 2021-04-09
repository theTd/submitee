package org.starrel.submitee.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.STemplate;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CreateServlet extends AbstractJsonServlet {
    @Override
    protected void request(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        if (!body.has("type")) {
            responseBadRequest(req, resp);
            return;
        }

        String type = body.get("type").getAsString();

        switch (type) {
            case "STemplate": {
                try {
                    STemplate template = SubmiteeServer.getInstance().createTemplate();
                    resp.setStatus(HttpStatus.OK_200);
                    resp.getWriter().println(template.getUniqueId());
                    return;
                } catch (Exception e) {
                    ExceptionReporting.report(CreateServlet.class, "creating template", e);
                    responseInternalError(req, resp);
                    return;
                }
            }
            case "Submission": {
                JsonElement ele;
                if (!body.has("content") || !(ele = body.get("content")).isJsonObject()) {
                    ExceptionReporting.report(CreateServlet.class, "parsing submit request",
                            "request body does not contains content, body=" + SubmiteeServer.GSON.toJson(body));
                    responseBadRequest(req, resp);
                    return;
                }
                JsonObject content = ele.getAsJsonObject();
                // TODO: 2021-03-25-0025
                break;
            }
            default: {
                ExceptionReporting.report(CreateServlet.class, "parsing type", "unknown type: " + type);
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                resp.getWriter().println(I18N.Http.INVALID_INPUT.format(req));
                return;
            }
        }
    }
}
