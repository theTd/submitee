package org.starrel.submitee.http;

import com.google.common.io.ByteStreams;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.blob.Blob;
import org.starrel.submitee.blob.SubmiteeFileItem;
import org.starrel.submitee.model.STemplateImpl;
import org.starrel.submitee.model.Session;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
        String blobStorageName = fieldAttributeMap.get("blob_storage", String.class);
        if (blobStorageName == null) {
            ExceptionReporting.report(UploadServlet.class, "blob_storage not defined",
                    String.format("tried to upload file to field %s:%s not defined blob-storage", template.getTemplateId(), fieldName));
            responseInternalError(req, resp);
            return;
        }

        FileUpload fileUpload = new FileUpload(new FileItemFactory() {
            boolean created = false;

            @Override
            public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                if (isFormField) return new IgnoredFileItem();
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
            List<FileItem> fileItems = fileUpload.parseRequest(new JakartaServletRequestContext(req));
            Blob uploaded = fileItems.stream().filter(i -> i instanceof SubmiteeFileItem)
                    .map(i -> ((SubmiteeFileItem) i).getBlob()).collect(Collectors.toList()).get(0);

            resp.setStatus(200);
            resp.setContentType("application/json");
            JsonWriter responseWriter = new JsonWriter(resp.getWriter());
            responseWriter.beginObject();
            responseWriter.name("url").value("/get-file/" + uploaded.getKey());
            responseWriter.name("key").value(uploaded.getKey());
            responseWriter.endObject();
            responseWriter.close();
        } catch (FileUploadException e) {
            ExceptionReporting.report(UploadServlet.class, "parsing upload request", e);
            responseInternalError(req, resp);
        }
    }

    private static class JakartaServletRequestContext implements RequestContext {
        private final HttpServletRequest request;

        private JakartaServletRequestContext(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String getCharacterEncoding() {
            return request.getCharacterEncoding();
        }

        @Override
        public String getContentType() {
            return request.getContentType();
        }

        @Override
        public int getContentLength() {
            return request.getContentLength();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return request.getInputStream();
        }
    }

    private static class IgnoredFileItem implements FileItem {

        @Override
        public InputStream getInputStream() throws IOException {
            return InputStream.nullInputStream();
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean isInMemory() {
            return false;
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public byte[] get() {
            return new byte[0];
        }

        @Override
        public String getString(String encoding) throws UnsupportedEncodingException {
            return null;
        }

        @Override
        public String getString() {
            return null;
        }

        @Override
        public void write(File file) throws Exception {

        }

        @Override
        public void delete() {

        }

        @Override
        public String getFieldName() {
            return null;
        }

        @Override
        public void setFieldName(String name) {

        }

        @Override
        public boolean isFormField() {
            return false;
        }

        @Override
        public void setFormField(boolean state) {

        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return OutputStream.nullOutputStream();
        }

        @Override
        public FileItemHeaders getHeaders() {
            return null;
        }

        @Override
        public void setHeaders(FileItemHeaders headers) {

        }
    }
}
