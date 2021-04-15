package org.starrel.submitee.http;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.*;
import org.starrel.submitee.model.STemplate;
import org.starrel.submitee.model.STemplateImpl;
import org.starrel.submitee.model.Submission;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CreateServlet extends AbstractJsonServlet {
    {
        setBaseUri("/create");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length < 1) {
            responseBadRequest(req, resp);
            return;
        }
        switch (uriParts[0]) {
            case "publish-template": {
                User user = getSession(req).getUser();
                if (!user.isSuperuser()) {
                    responseAccessDenied(req, resp);
                    return;
                }

                if (uriParts.length != 2) {
                    responseBadRequest(req, resp);
                    return;
                }
                UUID targetTemplate = UUID.fromString(uriParts[1]);
                STemplateImpl toPublish;
                try {
                    toPublish = SubmiteeServer.getInstance().getTemplate(targetTemplate);
                } catch (ExecutionException e) {
                    ExceptionReporting.report(CreateServlet.class, "fetching template info", e);
                    responseInternalError(req, resp);
                    return;
                }
                if (toPublish.isPublished()) {
                    responseClassifiedError(req, resp, ClassifiedErrors.TEMPLATE_ALREADY_PUBLISHED);
                    return;
                }

                List<STemplateImpl> allVersionTemplates;
                try {
                    allVersionTemplates = SubmiteeServer.getInstance().getTemplateKeeper()
                            .getAllVersionTemplates(toPublish.getTemplateId());
                } catch (ExecutionException e) {
                    ExceptionReporting.report(CreateServlet.class, "fetching template info", e);
                    responseInternalError(req, resp);
                    return;
                }
                Collections.sort(allVersionTemplates);

                for (STemplateImpl t : allVersionTemplates) {
                    if (t.isPublished()) {
                        if (t.getVersion() > toPublish.getVersion()) {
                            responseClassifiedError(req, resp, ClassifiedErrors.PUBLISH_OLDER_VERSION);
                            return;
                        }
                        t.setPublished(false);
                    }
                }

                toPublish.getAttributeMap().setAutoSaveAttribute(false);
                toPublish.setPublished(true);
                toPublish.setPublishedBy(getSession(req).getUser().getDescriptor());
                toPublish.setPublishTime(new Date());
                toPublish.getAttributeMap().setAutoSaveAttribute(true);
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            case "template": {
                User user = getSession(req).getUser();
                if (!user.isSuperuser()) {
                    responseAccessDenied(req, resp);
                    return;
                }

                try {
                    if (uriParts.length == 1) {
                        // create new
                        STemplate created = SubmiteeServer.getInstance().createTemplate();
                        resp.setStatus(HttpStatus.OK_200);
                        resp.setContentType("application/json");
                        resp.getWriter().println(SubmiteeServer.GSON.toJson(created.getUniqueId().toString()));
                    } else if (uriParts.length == 2) {
                        // create revision
                        String templateId = uriParts[1];
                        STemplateImpl revision = SubmiteeServer.getInstance().getTemplateLatestVersion(templateId);
                        if (revision == null) {
                            try {
                                UUID uuid = UUID.fromString(templateId);
                                revision = SubmiteeServer.getInstance().getTemplateLatestVersion(
                                        SubmiteeServer.getInstance().getTemplate(uuid).getTemplateId());
                            } catch (Exception ignored) {
                            }
                        }
                        if (revision == null) {
                            responseNotFound(req, resp);
                            return;
                        }
                        JsonObject content = JsonUtil.parseObject(body, "content");
                        if (content == null) {
                            // inherit
                            content = revision.getAttributeMap().toJsonTree().getAsJsonObject().deepCopy();
                        }
                        STemplateImpl revisionTemplate = SubmiteeServer.getInstance().getTemplateKeeper()
                                .createRevisionTemplate(revision.getTemplateId(), content);

                        resp.setStatus(HttpStatus.OK_200);
                        resp.setContentType("application/json");
                        resp.getWriter().println(SubmiteeServer.GSON.toJson(revisionTemplate.getUniqueId().toString()));
                    } else {
                        // bad uri
                        ExceptionReporting.report(CreateServlet.class, "parsing uri", "unrecognized uri: " + req.getRequestURI());
                        responseBadRequest(req, resp);
                    }
                } catch (ClassifiedException e) {
                    responseClassifiedError(req, resp, e.getClassifiedError());
                } catch (Exception e) {
                    ExceptionReporting.report(CreateServlet.class, "creating template", e);
                    responseInternalError(req, resp);
                }
                break;
            }
            case "submission": {
                // TODO: 2021/4/14 check template distinguish config

                if (uriParts.length != 2) {
                    ExceptionReporting.report(CreateServlet.class, "parsing uri",
                            "unrecognized uri: " + req.getRequestURI());
                    responseBadRequest(req, resp);
                    return;
                }

                String uuidString = uriParts[1];
                UUID templateUniqueId;
                try {
                    templateUniqueId = UUID.fromString(uuidString);
                } catch (Exception e) {
                    ExceptionReporting.report(CreateServlet.class, "parsing submission target",
                            "invalid uuid: " + uuidString);
                    responseBadRequest(req, resp);
                    return;
                }
                STemplate template;
                try {
                    template = SubmiteeServer.getInstance().getTemplate(templateUniqueId);
                    if (template == null) {
                        ExceptionReporting.report(CreateServlet.class, "target template not found",
                                "template uuid=" + templateUniqueId);
                        responseNotFound(req, resp);
                        return;
                    } else if (template.getLatestVersion() != template.getVersion()) {
                        responseClassifiedError(req, resp, ClassifiedErrors.SUBMIT_TO_OLD_TEMPLATE);
                        return;
                    }
                } catch (ExecutionException e) {
                    ExceptionReporting.report(CreateServlet.class, "fetching target template info", e);
                    responseInternalError(req, resp);
                    return;
                }

                // region check distinguish

                // endregion

                JsonObject submissionBody = JsonUtil.parseObject(body, "body");
                String debugInfo = JsonUtil.parseString(body, "debug");

                Submission submission = getSession(req).getUser().createSubmission(template);
                submission.getAttributeMap().setAutoSaveAttribute(false);
                submission.setBody(submissionBody);
                if (debugInfo != null) {
                    submission.setAttribute("debug", debugInfo);
                }
                submission.getAttributeMap().setAutoSaveAttribute(true);
                resp.setStatus(HttpStatus.OK_200);
                resp.setContentType("application/json");
                resp.getWriter().println(SubmiteeServer.GSON.toJson(submission.getUniqueId().toString()));
                break;
            }
            default: {
                ExceptionReporting.report(CreateServlet.class, "parsing method", "unknown method: " + uriParts[0]);
                responseBadRequest(req, resp);
                break;
            }
        }
    }
}
