package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.I18N;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CreateServlet extends JsonServlet {
    @Override
    protected void request(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        if (!body.has("type") ||
                !body.has("content") ||
                !body.get("content").isJsonObject()) {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            resp.getWriter().println(I18N.Http.INVALID_INPUT.format(req));
            return;
        }
        String type = body.get("type").getAsString();
        JsonObject content = body.get("content").getAsJsonObject();

        switch (type) {
            case "template": {
                // TODO: 2021-03-25-0025
                break;
            }
            case "submit": {
                // TODO: 2021-03-25-0025
                break;
            }
            default: {
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                resp.getWriter().println(I18N.Http.INVALID_INPUT.format(req));
                return;
            }
        }
    }
}
