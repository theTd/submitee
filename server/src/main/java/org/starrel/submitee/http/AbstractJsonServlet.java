package org.starrel.submitee.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class AbstractJsonServlet extends SubmiteeHttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"application/json".equalsIgnoreCase(req.getContentType())) {
            resp.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415);
            return;
        }
        JsonElement json;
        try {
            json = JsonParser.parseReader(new InputStreamReader(req.getInputStream()));
        } catch (Exception e) {
            ExceptionReporting.report("parsing post body", e);
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return;
        }
        if (!json.isJsonObject()) {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }
        request(req, resp, json.getAsJsonObject());
    }

    protected abstract void request(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException;
}
