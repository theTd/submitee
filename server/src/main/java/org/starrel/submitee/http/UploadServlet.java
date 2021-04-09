package org.starrel.submitee.http;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.blob.SubmiteeFileItem;
import org.starrel.submitee.model.STemplateImpl;
import org.starrel.submitee.model.Session;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class UploadServlet extends SubmiteeHttpServlet {
    {
        setBaseUri("/upload");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Session session = getSession(req);

        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length != 2) {
            ExceptionReporting.report(UploadServlet.class, "parsing uri", "unexpected uri: " + req.getRequestURI());
            responseBadRequest(req, resp);
            return;
        }

        String templateUniqueIdString = uriParts[0];
        String fieldName = uriParts[1];

        UUID templateUniqueId;
        try {
            templateUniqueId = UUID.fromString(templateUniqueIdString);
        } catch (Exception e) {
            ExceptionReporting.report(UploadServlet.class, "parsing uuid",
                    "invalid uuid: " + templateUniqueIdString);
            responseBadRequest(req, resp);
            return;
        }

        STemplateImpl template;
        try {
            template = SubmiteeServer.getInstance().getTemplateFromUUID(templateUniqueId);
        } catch (ExecutionException e) {
            ExceptionReporting.report(UploadServlet.class, "template not found",
                    "unknown template with uuid: " + templateUniqueId);
            responseInternalError(req, resp);
            return;
        }

        SFieldImpl targetField = template.getFields().get(fieldName);
        if (targetField == null) {
            ExceptionReporting.report(UploadServlet.class, "target field not found",
                    String.format("cannot find field: %s:%s", template.getTemplateId(), fieldName));
            responseInternalError(req, resp);
            return;
        }

        AttributeMap<SFieldImpl> fieldAttributeMap = targetField.getAttributeMap();
        String blobStorageName = fieldAttributeMap.get("blob-storage", String.class);
        if (blobStorageName == null) {
            ExceptionReporting.report(UploadServlet.class, "blob-storage not defined",
                    String.format("tried to upload file to field %s:%s not defined blob-storage", template.getTemplateId(), fieldName));
            responseInternalError(req, resp);
            return;
        }

        ServletFileUpload servletFileUpload = new ServletFileUpload(new FileItemFactory() {
            boolean created = false;

            @Override
            public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                if (created) throw new RuntimeException("uploading multiple file");
                try {
                    FileItem item = new SubmiteeFileItem(SubmiteeServer.getInstance().createBlob(
                            blobStorageName, fileName, contentType, session.getUser().getDescriptor()));
                    created = true;
                    return item;
                } catch (Exception e) {
                    throw new RuntimeException("failed creating blob", e);
                }
            }
        });
        try {
            List<FileItem> fileItems = servletFileUpload.parseRequest(req);
            SubmiteeFileItem uploaded = (SubmiteeFileItem) fileItems.get(0);

            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.getWriter().println(uploaded.getBlob().getBlobId());
            resp.getWriter().close();
        } catch (FileUploadException e) {
            ExceptionReporting.report(UploadServlet.class, "parsing upload request", e);
            responseInternalError(req, resp);
        }
    }
}
