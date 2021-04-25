package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.*;
import org.starrel.submitee.model.STemplateImpl;
import org.starrel.submitee.model.Submission;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PasteServlet extends AbstractJsonServlet {
    {
        setBaseUri("/paste");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        User user = getSession(req).getUser();
        if (!user.isSuperuser()) {
            responseAccessDenied(req, resp);
            return;
        }

        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length != 2) {
            ExceptionReporting.report(PasteServlet.class, "parsing uri", "unexpected uri: " + req.getRequestURI());
            responseBadRequest(req, resp);
            return;
        }
        String target = uriParts[0];
        UUID uuid;
        try {
            uuid = UUID.fromString(uriParts[1]);
        } catch (Exception e) {
            ExceptionReporting.report(PasteServlet.class, "parsing uuid", "uuid=" + uriParts[1], e);
            responseBadRequest(req, resp);
            return;
        }

        switch (target) {
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

                    String descText = JsonUtil.parseString(body, "desc");
                    if (Util.isEmptyHtml(descText)) {
                        body.remove("desc");
                    }

                    template.getAttributeMap().setAutoSaveAttribute(false);
                    template.getAttributeMap().set("", body);
                    for (SFieldImpl f : template.getFields().values()) {
                        if (Util.isEmptyHtml(f.getComment())) f.setComment(null);
                    }
                    template.getAttributeMap().setAutoSaveAttribute(true);
                    // TODO: 2021-04-06-0006 switch to apply method
                    resp.setStatus(HttpStatus.OK_200);
                } catch (Exception e) {
                    ExceptionReporting.report(PasteServlet.class, "applying content", e);
                    responseInternalError(req, resp);
                }
                break;
            }
            case "submission": {
                Submission submission;
                try {
                    submission = SubmiteeServer.getInstance().getSubmission(uuid);
                } catch (ExecutionException e) {
                    ExceptionReporting.report(PasteServlet.class, "fetching submission", e);
                    responseInternalError(req, resp);
                    return;
                }
                if (submission == null) {
                    responseNotFound(req, resp);
                    return;
                }

                submission.getAttributeMap().set("", body);
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            default: {
                ExceptionReporting.report(PasteServlet.class, "unknown target", "unknown target: " + target + ", uuid=" + uuid);
                responseBadRequest(req, resp);
                break;
            }
        }
    }
}
