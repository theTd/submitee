package org.starrel.submitee.http;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ClassifiedErrors;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.STemplateImpl;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class TemplateControlServlet extends SubmiteeHttpServlet {

    {
        setBaseUri("/template-control");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSession(req).getUser();
        if (!user.isSuperuser()) {
            responseAccessDenied(req, resp);
            return;
        }

        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length < 2) {
            responseBadRequest(req, resp);
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uriParts[0]);
        } catch (Exception e) {
            responseBadRequest(req, resp);
            return;
        }

        STemplateImpl template;
        try {
            template = SubmiteeServer.getInstance().getTemplate(uuid);
            if (template == null) {
                responseNotFound(req, resp);
                return;
            }
        } catch (ExecutionException e) {
            ExceptionReporting.report(CreateServlet.class, "fetching template info", e);
            responseInternalError(req, resp);
            return;
        }
        switch (uriParts[1]) {
            case "archive": {
                if (template.getAttributeMap().get("published", Boolean.class, false)) {
                    responseClassifiedError(req, resp, ClassifiedErrors.TEMPLATE_ALREADY_PUBLISHED);
                    return;
                }
                template.getAttributeMap().set("archived", true);
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            case "cancel": {
                if (!template.getAttributeMap().get("published", Boolean.class, false)) {
                    responseClassifiedError(req, resp, ClassifiedErrors.TEMPLATE_NOT_PUBLISHED);
                    return;
                }
                template.getAttributeMap().set("published", false);
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            case "publish": {
                if (template.isPublished()) {
                    responseClassifiedError(req, resp, ClassifiedErrors.TEMPLATE_ALREADY_PUBLISHED);
                    return;
                }

                List<STemplateImpl> allVersionTemplates;
                try {
                    allVersionTemplates = SubmiteeServer.getInstance().getTemplateKeeper()
                            .getAllVersionTemplates(template.getTemplateId());
                } catch (ExecutionException e) {
                    ExceptionReporting.report(CreateServlet.class, "fetching template info", e);
                    responseInternalError(req, resp);
                    return;
                }
                Collections.sort(allVersionTemplates);

                for (STemplateImpl t : allVersionTemplates) {
                    if (t.isPublished()) {
                        if (t.getVersion() > template.getVersion()) {
                            responseClassifiedError(req, resp, ClassifiedErrors.PUBLISH_OLDER_VERSION);
                            return;
                        }
                        t.setPublished(false);
                    }
                }

                template.getAttributeMap().setAutoSaveAttribute(false);
                template.setPublished(true);
                template.setPublishedBy(getSession(req).getUser().getDescriptor());
                template.setPublishTime(new Date());
                template.getAttributeMap().setAutoSaveAttribute(true);
                resp.setStatus(HttpStatus.OK_200);
                break;
            }
            default: {
                responseBadRequest(req, resp);
                break;
            }
        }
    }
}
