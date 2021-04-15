package org.starrel.submitee.http;

import com.google.common.io.ByteStreams;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.blob.Blob;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class GetFileServlet extends SubmiteeHttpServlet {
    {
        setBaseUri("/get-file");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding(null);
        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length < 1) {
            ExceptionReporting.report(GetFileServlet.class, "parsing uri", "unexpected uri: " + req.getRequestURI());
            responseBadRequest(req, resp);
            return;
        }

        String blobKey = uriParts[0];

        try {
            Blob blob = SubmiteeServer.getInstance().getBlobByKey(blobKey);
            if (blob == null) {
                ExceptionReporting.report(GetFileServlet.class, "blob not found",
                        "blob not found with key: " + blobKey);
                responseNotFound(req, resp);
                return;
            }

            // region access check
            User user = getSession(req).getUser();
            if (!user.isSuperuser() && !blob.getUploader().equals(user.getDescriptor())) {
                responseAccessDenied(req, resp);
                return;
            }
            // endregion

            if (uriParts.length > 1) {
                // region metadata
                switch (uriParts[1]) {
                    case "metadata": {
                        Map<String, Object> metadata = new LinkedHashMap<>();
                        metadata.put("content-type", blob.getContentType());
                        metadata.put("size", blob.getSize());
                        metadata.put("filename", blob.getFilename());
                        metadata.put("uploader", blob.getUploader().toString());
                        metadata.put("create-time", blob.getCreateTime().getTime());
                        resp.setStatus(HttpStatus.OK_200);
                        resp.setContentType("application/json");
                        resp.getWriter().println(SubmiteeServer.GSON.toJson(metadata));
                        break;
                    }
                    default: {
                        responseBadRequest(req, resp);
                        break;
                    }
                }
                // endregion
            } else {
                // region start file stream
                resp.setStatus(HttpStatus.OK_200);
                resp.setContentType(blob.getContentType());
                resp.addHeader("Content-Disposition", "attachment; filename=\"" + blob.getFilename() + "\"");
                resp.setContentLengthLong(blob.getSize());
                ByteStreams.copy(blob.getInputStream(), resp.getOutputStream());
                // endregion
            }
        } catch (Exception e) {
            ExceptionReporting.report(GetFileServlet.class, "fetch blob info", e);
            responseInternalError(req, resp);
        }
    }
}
