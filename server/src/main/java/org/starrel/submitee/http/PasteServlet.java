package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.I18N;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PasteServlet extends JsonServlet {
    @Override
    protected void request(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        if (!body.has("target") ||
                !body.has("content") ||
                !body.get("content").isJsonObject()) {
            resp.setStatus(HttpStatus.NOT_ACCEPTABLE_406);
            resp.getWriter().println(I18N.Http.INVALID_INPUT.format(req));
            return;
        }
        String target = body.get("target").getAsString();
        JsonObject content = body.get("content").getAsJsonObject();

        resp.setStatus(200);
    }
}
