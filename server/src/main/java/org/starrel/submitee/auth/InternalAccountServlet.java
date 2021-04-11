package org.starrel.submitee.auth;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.*;
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
        String grecaptchaSitekey = SubmiteeServer.getInstance().getAttribute("grecaptcha-sitekey", String.class);
        String grecaptchaSecretKey = SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class);
        if (grecaptchaSitekey != null && grecaptchaSecretKey != null) {
            writer.name("grecaptcha-sitekey").value(grecaptchaSitekey);
        }
        writer.endObject();
        writer.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, JsonObject body) throws ServletException, IOException {
        String[] uriParts = parseUri(req.getRequestURI());
        if (uriParts.length < 1) {
            responseBadRequest(req, resp);
            return;
        }

        switch (uriParts[0]) {
            case "send-verify-code": {
                boolean grecaptcha = Util.grecaptchaConfigured();
                String email = body.has("mail") ? body.get("mail").getAsString() : null;
                String token = null;
                if (grecaptcha) {
                    token = body.has("captcha") ? body.get("captcha").getAsString() : null;
                }

                if (email == null) {
                    responseBadRequest(req, resp);
                    return;
                }
                if (grecaptcha && token == null) {
                    responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.REQUIRE_CAPTCHA.format(req));
                    return;
                }
                String finalToken = token;
                req.startAsync().start(() -> {
                    try {
                        if (grecaptcha) {
                            if (!Util.grecaptchaVerify(finalToken, Util.getRemoteAddr(req),
                                    SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class))) {
                                responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.CAPTCHA_FAILURE.format(req));
                                return;
                            }
                        }
                        // TODO: 2021-04-12-0012
                        Util.sendTemplatedEmail();
                    } catch (Exception e) {
                        ExceptionReporting.report(InternalAccountServlet.class, "processing send-verify-code", e);
                        try {
                            responseInternalError(req, resp);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
            }
        }
    }
}
