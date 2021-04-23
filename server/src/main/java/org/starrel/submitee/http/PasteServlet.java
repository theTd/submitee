package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.jsoup.Jsoup;
import org.starrel.submitee.ClassifiedErrors;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.JsonUtil;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.STemplateImpl;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PasteServlet extends AbstractJsonServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
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
                    if (template.isPublished()) {
                        responseClassifiedError(req, resp, ClassifiedErrors.TEMPLATE_ALREADY_PUBLISHED);
                        return;
                    }
                    if (template.getPublishTime() != null) {
                        responseClassifiedError(req, resp, ClassifiedErrors.EVER_PUBLISHED_TEMPLATE);
                        return;
                    }

                    String descText = JsonUtil.parseString(content, "desc");
                    if (descText != null) {
                        if (descText.isEmpty()) {
                            content.remove("desc");
                        } else {
                            try {
                                String text = Jsoup.parse(descText).text();
                                if (text == null || text.isEmpty()) {
                                    content.remove("desc");
                                }
                            } catch (Exception e) {
                                ExceptionReporting.report(PasteServlet.class, "parsing desc html", e);
                                content.remove("desc");
                            }
                        }
                    }

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
