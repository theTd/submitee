package org.starrel.submitee.http;

import com.google.gson.stream.JsonWriter;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.SubmiteeServer;

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
            return;
        }
        String uuid = uri[1];
        UUID uniqueId;
        try {
            uniqueId = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }
        Object object = SubmiteeServer.getInstance().getObjectFromUUID(uniqueId);

        resp.setStatus(200);
        resp.setContentType("application/json");
        JsonWriter jsonWriter = new JsonWriter(resp.getWriter());
        jsonWriter.beginObject();
        // TODO: 2021/3/26 check permissions
        if (object == null) {
            jsonWriter.name("scheme").nullValue();
        } else {
            jsonWriter.name("scheme").value("something");
            // TODO: 2021/3/26 write data
        }
        jsonWriter.endObject();
        jsonWriter.close();
    }
}
