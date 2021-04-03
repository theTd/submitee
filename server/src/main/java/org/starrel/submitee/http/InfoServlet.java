package org.starrel.submitee.http;

import com.google.gson.stream.JsonWriter;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class InfoServlet extends SubmiteeHttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] uri = parseUri(req.getRequestURI());
        if (uri.length != 2) {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            ExceptionReporting.report("invalid request uri in InfoServlet uri=" + req.getRequestURI());
            return;
        }
        String uuid = uri[1];
        UUID uniqueId;
        try {
            uniqueId = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            ExceptionReporting.report("invalid request parameter in InfoServlet uuid=" + uuid);
            return;
        }
        Object object = SubmiteeServer.getInstance().getObjectFromUUID(uniqueId);
        if (object == null) {
            resp.setStatus(HttpStatus.NOT_FOUND_404);
            ExceptionReporting.report("invalid request parameter in InfoServlet uuid=" + uuid);
            return;
        }

        String scheme;
        if (!(object instanceof AttributeHolder) || (scheme = ((AttributeHolder<?>) object).getAttributeScheme()) == null) {
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            resp.getWriter().println(I18N.Http.INTERNAL_ERROR.format(req));
            ExceptionReporting.report("unexpected object received in InfoServlet uuid=" + uniqueId + ", object=" + object);
            return;
        }

        resp.setStatus(HttpStatus.OK_200);
        resp.setContentType("application/json");

        JsonWriter jsonWriter = new JsonWriter(resp.getWriter());
        jsonWriter.beginObject();
        // TODO: 2021/3/26 check ACLs
        jsonWriter.name("scheme").value(scheme);
        jsonWriter.name("attributes").jsonValue(SubmiteeServer.GSON.toJson(((AttributeHolder<?>) object)
                .getAttributeMap().toJson(path -> !path.equalsIgnoreCase("protected"))));
        jsonWriter.endObject();
        jsonWriter.close();
    }
}
