package org.starrel.submitee.http;

import com.google.gson.stream.JsonWriter;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeHolder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class InfoServlet extends SubmiteeHttpServlet {
    {
        setBaseUri("/info");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] uri = parseUri(req.getRequestURI());
        if (uri.length != 1) {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            ExceptionReporting.report(InfoServlet.class, "parsing parameter", "unexpected uri: " + req.getRequestURI());
            return;
        }
        String uuidString = uri[0];
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            ExceptionReporting.report(InfoServlet.class, "parsing parameter", "unexpected uuidString: " + uuidString);
            responseBadRequest(req, resp);
            return;
        }
        String type;
        try {
            type = SubmiteeServer.getInstance().getObjectMapController().getType(uuid);
        } catch (ExecutionException e) {
            ExceptionReporting.report(InfoServlet.class, "querying object type from uuid", e);
            responseInternalError(req, resp);
            return;
        }
        if (type == null) {
            ExceptionReporting.report(InfoServlet.class, "fetching resource", "missing object, uuid=" + uuid);
            responseNotFound(req, resp);
            return;
        }

        Object object = null;
        try {
            switch (type) {
                case "template": {
                    object = SubmiteeServer.getInstance().getTemplate(uuid);
                    break;
                }
                case "submission": {
                    // TODO: 2021-04-05-0005
                    break;
                }
                default: {
                    ExceptionReporting.report(InfoServlet.class, "fetching object from uuid", "unknown type: " + type);
                    responseInternalError(req, resp);
                    return;
                }
            }
            if (object == null) throw new NullPointerException("unable to fetch object, type=" + type);
        } catch (Exception e) {
            ExceptionReporting.report(InfoServlet.class, "fetching object from uuid", e);
            responseInternalError(req, resp);
            return;
        }

        String scheme;
        if ((scheme = ((AttributeHolder<?>) object).getAttributeScheme()) == null) {
            ExceptionReporting.report(InfoServlet.class, "serializing resource",
                    "requested object is not instance of attribute holder or attribute holder returns null scheme, uuidString=" + uuid);
            responseInternalError(req, resp);
            return;
        }

        resp.setStatus(HttpStatus.OK_200);
        resp.setContentType("application/json");

        JsonWriter jsonWriter = new JsonWriter(resp.getWriter());
        jsonWriter.beginObject();
        // TODO: 2021/3/26 check ACLs
        jsonWriter.name("scheme").value(scheme);
        jsonWriter.name("body").jsonValue(SubmiteeServer.GSON.toJson(((AttributeHolder<?>) object)
                .getAttributeMap().toJsonTree(path -> !path.equalsIgnoreCase("protected"))));
        jsonWriter.endObject();
        jsonWriter.close();
    }
}
