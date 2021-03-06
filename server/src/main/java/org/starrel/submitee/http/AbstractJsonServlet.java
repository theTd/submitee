package org.starrel.submitee.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStreamReader;

public abstract class AbstractJsonServlet extends SubmiteeHttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"application/json".equalsIgnoreCase(req.getContentType())) {
            ExceptionReporting.report(AbstractJsonServlet.class,
                    "parsing request body", "expected content type application/json, got " + req.getContentType());
            resp.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415);
            return;
        }

        JsonElement json;
        try {
            json = JsonParser.parseReader(new InputStreamReader(req.getInputStream()));
        } catch (Exception e) {
            ExceptionReporting.report(AbstractJsonServlet.class, "parsing request body", e);
            responseBadRequest(req, resp);
            return;
        }
        if (json == null || json.isJsonNull()) json = new JsonObject();
        if (!json.isJsonObject()) {
            ExceptionReporting.report(AbstractJsonServlet.class, "parsing request body", "expected json object, got " + json);
            responseBadRequest(req, resp);
            return;
        }
        doPost(req, resp, json.getAsJsonObject());
    }

    protected abstract void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException;
}
