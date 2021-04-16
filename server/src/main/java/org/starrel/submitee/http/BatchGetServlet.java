package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.JsonUtil;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.STemplateImpl;
import org.starrel.submitee.model.SubmissionImpl;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class BatchGetServlet extends AbstractJsonServlet {
    {
        setBaseUri("/batch-get");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length < 1) {
            responseBadRequest(req, resp);
            return;
        }

        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(() -> {
            JsonObject filter = JsonUtil.parseObject(body, "filter");
            if (filter == null) filter = new JsonObject();
            try {
                switch (uriParts[0]) {
                    case "template": {
                        User user = getSession(req).getUser();
                        if (!user.isSuperuser()) {
                            responseAccessDenied(req, resp);
                            return;
                        }

                        boolean latest = body.has("latest") && body.get("latest").getAsBoolean();
                        boolean getSize = uriParts.length == 2 && uriParts[1].equalsIgnoreCase("size");

                        try {
                            Document query = Document.parse(SubmiteeServer.GSON.toJson(filter));
                            applyPath(query);
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

                            if (getSize) {
                                resp.getWriter().println(SubmiteeServer.GSON.toJson(list.size()));
                                return;
                            }
                            Integer start = JsonUtil.parseInt(body, "start");
                            Integer length = start == null ?
                                    ((Integer) list.size()) :
                                    JsonUtil.parseInt(body, "length");
                            if (length == null) length = list.size();

                            JsonWriter jsonWriter = new JsonWriter(resp.getWriter());
                            jsonWriter.beginArray();

                            Iterator<STemplateImpl> ite;
                            if (start != null) {
                                ite = list.listIterator(start);
                            } else {
                                ite = list.listIterator();
                            }

                            int pass = 0;
                            while (ite.hasNext() && pass++ < length) {
                                STemplateImpl template = ite.next();
                                jsonWriter.beginObject();
                                jsonWriter.name("scheme").value(template.getAttributeScheme());
                                jsonWriter.name("body").jsonValue(SubmiteeServer.GSON.toJson(template.getAttributeMap().toJsonTree()));
                                jsonWriter.endObject();
                            }

                            jsonWriter.endArray();
                            jsonWriter.close();
                        } catch (Exception e) {
                            ExceptionReporting.report(BatchGetServlet.class, "fetching templates", e);
                            responseInternalError(req, resp);
                        }
                        break;
                    }
                    case "submission": {
                        User user = getSession(req).getUser();
                        if (!user.isSuperuser()) {
                            // TODO: 2021-04-14-0014 allows per use control
                            responseAccessDenied(req, resp);
                            return;
                        }

                        try {
                            Document query = Document.parse(SubmiteeServer.GSON.toJson(filter));
                            applyPath(query);
                            // TODO: 2021-04-14-0014 pagination
                            List<SubmissionImpl> list = SubmiteeServer.getInstance().getSubmissions(query);

                            resp.setStatus(HttpStatus.OK_200);
                            resp.setContentType("application/json");

                            JsonWriter jsonWriter = new JsonWriter(resp.getWriter());
                            jsonWriter.beginArray();
                            for (SubmissionImpl submission : list) {
                                jsonWriter.beginObject();
                                jsonWriter.name("scheme").value(submission.getAttributeScheme());
                                jsonWriter.name("body").jsonValue(SubmiteeServer.GSON.toJson(submission.getAttributeMap().toJsonTree()));
                                jsonWriter.endObject();
                            }
                            jsonWriter.endArray();
                            jsonWriter.close();
                        } catch (Exception e) {
                            ExceptionReporting.report(BatchGetServlet.class, "fetching templates", e);
                            responseInternalError(req, resp);
                        }
                        break;
                    }
                    default: {
                        responseBadRequest(req, resp);
                    }
                }
            } finally {
                asyncContext.complete();
            }
        });
    }

    private static Object applyPath(Object documentOrArray) {
        if (documentOrArray instanceof Document) {
            Document document = ((Document) documentOrArray);
            for (String key : document.keySet()) {
                if (key.startsWith("$")) {
                    document.put(key, applyPath(document.get(key)));
                } else {
                    document.put("body." + key, applyPath(document.remove(key)));
                }
            }
            return document;
        } else if (documentOrArray instanceof List) {
            //noinspection unchecked
            List<Document> array = (List<Document>) documentOrArray;
            ListIterator<Document> iterator = array.listIterator();
            while (iterator.hasNext()) {
                iterator.set((Document) applyPath(iterator.next()));
            }
            return array;
        } else {
            return documentOrArray;
        }
    }
}
