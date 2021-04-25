package org.starrel.submitee.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.model.STemplateImpl;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

public class BatchGetServlet extends AbstractJsonServlet {
    {
        setBaseUri("/batch-get");
    }

    private final static Predicate<String> TEMPLATE_ABBREV_FILTER = s ->
            s.equalsIgnoreCase("uuid") ||
                    s.equalsIgnoreCase("grouping") ||
                    s.equalsIgnoreCase("template-id") ||
                    s.equalsIgnoreCase("version") ||
                    s.equalsIgnoreCase("published") ||
                    s.equalsIgnoreCase("name") ||
                    s.equalsIgnoreCase("comment") ||
                    s.equalsIgnoreCase("publish-time") ||
                    s.equalsIgnoreCase("archived");
    private final static Predicate<String> SUBMISSION_ABBREV_FILTER = s ->
            s.equalsIgnoreCase("unique-id") ||
                    s.equalsIgnoreCase("template-uuid") ||
                    s.equalsIgnoreCase("submit-user") ||
                    s.equalsIgnoreCase("submit-time");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length < 1) {
            responseBadRequest(req, resp);
            return;
        }

        User user = getSession(req).getUser();
        if (!user.isSuperuser()) {
            responseAccessDenied(req, resp);
            return;
        }

        AsyncContext asyncContext = req.startAsync();
        asyncContext.start(() -> {
            try {
                JsonObject filter = JsonUtil.parseObject(body, "filter");
                if (filter == null) filter = new JsonObject();

                JsonObject order = JsonUtil.parseObject(body, "order");

                JsonArray idList = JsonUtil.parseArray(body, "list");

                boolean getSize = uriParts.length == 2 && uriParts[1].equalsIgnoreCase("size");

                Integer start = JsonUtil.parseInt(body, "start");
                if (start == null) start = 0;
                Integer length = JsonUtil.parseInt(body, "length");
                Boolean abbrev = JsonUtil.parseBoolean(body, "abbrev");
                if (abbrev == null) abbrev = false;

                List<AttributeHolder<?>> objects = new LinkedList<>();
                Predicate<String> abbrevFilter;
                switch (uriParts[0]) {
                    case "template": {
                        abbrevFilter = TEMPLATE_ABBREV_FILTER;
                        if (idList != null) {
                            for (JsonElement id : idList) {
                                UUID uuid;
                                if (!id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()) {
                                    ExceptionReporting.report(BatchGetServlet.class, "parsing id list",
                                            "unexpected id list:" + SubmiteeServer.GSON.toJson(idList));
                                    responseBadRequest(req, resp);
                                    return;
                                }
                                uuid = UUID.fromString(id.getAsJsonPrimitive().getAsString());

                                STemplateImpl t;
                                try {
                                    t = SubmiteeServer.getInstance().getTemplateKeeper().getTemplate(uuid);
                                } catch (ExecutionException e) {
                                    ExceptionReporting.report(BatchGetServlet.class, "fetching templates",
                                            "failed fetching template with uuid=" + uuid, e);
                                    responseInternalError(req, resp);
                                    return;
                                }
                                if (t != null) objects.add(t);
                            }
                        } else {
                            boolean latest = body.has("latest") && body.get("latest").getAsBoolean();

                            try {
                                Document query = Document.parse(SubmiteeServer.GSON.toJson(filter));
                                query = (Document) patchSelector(query);
                                objects.addAll(SubmiteeServer.getInstance().getTemplateKeeper().getByQuery(query));

                                if (latest) {
                                    Iterator<AttributeHolder<?>> ite = objects.iterator();
                                    while (ite.hasNext()) {
                                        STemplateImpl c = (STemplateImpl) ite.next();
                                        if (c.getLatestVersion() != c.getVersion()) ite.remove();
                                    }
                                }
                            } catch (Exception e) {
                                ExceptionReporting.report(BatchGetServlet.class, "fetching templates", e);
                                responseInternalError(req, resp);
                                return;
                            }
                        }
                        break;
                    }
                    case "submission": {
                        abbrevFilter = SUBMISSION_ABBREV_FILTER;
                        try {
                            Document query = Document.parse(SubmiteeServer.GSON.toJson(filter));
                            query = (Document) patchSelector(query);
                            Document orderBson = null;
                            if (order != null) {
                                orderBson = Document.parse(SubmiteeServer.GSON.toJson(order));
                                orderBson = (Document) patchSelector(orderBson);
                            }
                            objects.addAll(SubmiteeServer.getInstance().getSubmissions(query, orderBson));
                        } catch (Exception e) {
                            ExceptionReporting.report(BatchGetServlet.class, "fetching submissions", e);
                            responseInternalError(req, resp);
                            return;
                        }
                        break;
                    }
                    default: {
                        responseBadRequest(req, resp);
                        return;
                    }
                }

                try {
                    if (getSize) {
                        resp.getWriter().println(SubmiteeServer.GSON.toJson(objects.size()));
                        return;
                    }

                    resp.setStatus(HttpStatus.OK_200);
                    resp.setContentType("application/json");

                    JsonWriter jsonWriter = new JsonWriter(resp.getWriter());
                    jsonWriter.beginArray();

                    Iterator<AttributeHolder<?>> ite = objects.listIterator(start);

                    int pass = 0;
                    while (ite.hasNext()) {
                        if (length != null && ++pass > length) break;

                        AttributeHolder<?> obj = ite.next();
                        jsonWriter.beginObject();
                        jsonWriter.name("scheme").value(obj.getAttributeScheme());
                        jsonWriter.name("body").jsonValue(SubmiteeServer.GSON.toJson(
                                abbrev ? obj.getAttributeMap().toJsonTree(abbrevFilter) : obj.getAttributeMap().toJsonTree()
                        ));
                        jsonWriter.endObject();
                    }

                    jsonWriter.endArray();
                    jsonWriter.close();
                } catch (Exception e) {
                    ExceptionReporting.report(BatchGetServlet.class, "writing response", e);
                    responseInternalError(req, resp);
                }
            } finally {
                asyncContext.complete();
            }
        });
    }

    private static Object patchSelector(Object documentOrArray) {
        if (documentOrArray instanceof Document) {
            Document document = ((Document) documentOrArray);
            for (String key : new ArrayList<>(document.keySet())) {
                if (key.startsWith("$")) {
                    document.put(key, patchSelector(document.get(key)));
                } else {
                    document.put("body." + key, patchSelector(document.remove(key)));
                }
            }
            return document;
        } else if (documentOrArray instanceof List) {
            //noinspection unchecked
            ListIterator<Object> iterator = ((List<Object>) documentOrArray).listIterator();
            while (iterator.hasNext()) {
                iterator.set(patchSelector(iterator.next()));
            }
            return documentOrArray;
        } else {
            return documentOrArray;
        }
    }
}
