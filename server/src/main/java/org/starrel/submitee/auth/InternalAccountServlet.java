package org.starrel.submitee.auth;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.http.AbstractJsonServlet;

import java.io.IOException;

public class InternalAccountServlet extends AbstractJsonServlet {
    {
        setBaseUri("/internal-account");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean registerEnabled = SubmiteeServer.getInstance().getAttribute(
                "register-enabled", Boolean.class, true);
        String registerDisableMessage = SubmiteeServer.getInstance().getAttribute(
                "register-disable-message", String.class);

        resp.setStatus(HttpStatus.OK_200);
        resp.setContentType("application/json");

        JsonWriter writer = new JsonWriter(resp.getWriter());
        writer.beginObject();
        writer.name("register-enabled").value(registerEnabled);
        if (registerDisableMessage != null) {
            writer.name("register-disable-message").value(registerDisableMessage);
        }
        writer.endObject();
        writer.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
    }
}
