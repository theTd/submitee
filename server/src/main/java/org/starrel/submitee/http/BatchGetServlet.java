package org.starrel.submitee.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.bson.Document;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.STemplateImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BatchGetServlet extends AbstractJsonServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        JsonElement t;

        if (!body.has("scheme") || !(t = body.get("scheme")).isJsonPrimitive()) {
            ExceptionReporting.report(BatchGetServlet.class, "parsing parameters",
                    "unexpected request body: " + SubmiteeServer.GSON.toJson(body));
            responseBadRequest(req, resp);
            return;
        }
        String scheme = t.getAsString();
        JsonObject filter = new JsonObject();

        if (body.has("filter")) {
            if (!(t = body.get("filter")).isJsonObject()) {
                ExceptionReporting.report(BatchGetServlet.class, "parsing parameters",
                        "unexpected request body: " + SubmiteeServer.GSON.toJson(body));
                responseBadRequest(req, resp);
                return;
            }
            filter = t.getAsJsonObject();
        }

        switch (scheme) {
            case "STemplate": {
                boolean latest = body.has("latest") && body.get("latest").getAsBoolean();

                try {
                    Document query = Document.parse(SubmiteeServer.GSON.toJson(filter));
                    alterPath(query);
                    List<STemplateImpl> list = SubmiteeServer.getInstance().getTemplateKeeper().getByQuery(query);
                    if (latest) {
                        Iterator<STemplateImpl> ite = list.iterator();
                        while (ite.hasNext()) {
                            STemplateImpl c = ite.next();
                            if (c.getLatestVersion() != c.getVersion()) ite.remove();
                        }
                    }

                    resp.setStatus(HttpStatus.OK_200);
                    resp.setContentType("application/json");

                    JsonWriter jsonWriter = new JsonWriter(resp.getWriter());
                    jsonWriter.beginArray();
                    for (STemplateImpl template : list) {
                        jsonWriter.beginObject();
                        jsonWriter.name("scheme").value(template.getAttributeScheme());
                        jsonWriter.name("body").jsonValue(SubmiteeServer.GSON.toJson(template.getAttributeMap().toJsonTree()));
                        jsonWriter.endObject();
                    }
                    jsonWriter.endArray();
                    jsonWriter.close();
                } catch (ExecutionException e) {
                    ExceptionReporting.report(BatchGetServlet.class, "fetching templates", e);
                    responseInternalError(req, resp);
                }
                return;
            }
            case "Submission": {
                resp.setStatus(HttpStatus.OK_200);
                resp.setContentType("application/json");
                // TODO: 2021-04-05-0005
                return;
            }
        }
    }

    private static void alterPath(Document query) {
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (!entry.getKey().startsWith("$")) {
                if (entry.getValue() instanceof Document) {
                    alterPath(((Document) entry.getValue()));
                }
                query.put("body." + entry.getKey(), entry.getValue());
                query.remove(entry.getKey());
            }
        }
    }
}
