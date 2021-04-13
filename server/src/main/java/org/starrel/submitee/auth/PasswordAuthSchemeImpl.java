package org.starrel.submitee.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import org.starrel.submitee.*;
import org.starrel.submitee.model.Session;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PasswordAuthSchemeImpl implements PasswordAuthScheme {
    private final static String RESULT_BAD_REQUEST = "bad_request";
    private final static String RESULT_INTERNAL_ERROR = "internal_error";
    private final static String RESULT_REQUIRE_CAPTCHA = "require_captcha";
    private final static String RESULT_CAPTCHA_FAILURE = "captcha_failure";

    private AuthHandler authHandler;

    @Override
    public void setHandler(AuthHandler handler) {
        this.authHandler = handler;
    }

    @Override
    public String getViewUrl(Session session) {
        return "/static/auth-scheme-password.html";
    }

    @SneakyThrows
    @Override
    public Map<String, String> getParams(Session session) {
        Map<String, String> params = new LinkedHashMap<>();
        String rst = authHandler.getResetPasswordLink();
        if (rst != null) params.put("rst", Base64.getEncoder().encodeToString(rst.getBytes(StandardCharsets.UTF_8)));
        if (Util.grecaptchaConfigured()) {
            params.put("g", SubmiteeServer.getInstance().getAttribute("grecaptcha-sitekey", String.class));
        }
        return params;
    }

    @Override
    public AuthResult auth(Session session, JsonElement content) {
        if (authHandler == null) {
            ExceptionReporting.report(PasswordAuthSchemeImpl.class, "authenticating user", "auth handler not set");
            return new AbstractAuthResult(RESULT_INTERNAL_ERROR,
                    I18N.Http.INTERNAL_ERROR.format(session.getUser().getPreferredLanguage()), null);
        }

        if (!content.isJsonObject()) {
            ExceptionReporting.report(PasswordAuthSchemeImpl.class, "authenticating user",
                    "unexpected request body: " + SubmiteeServer.GSON.toJson(content));
            return new AbstractAuthResult(RESULT_BAD_REQUEST,
                    I18N.Http.INVALID_INPUT.format(session.getUser().getPreferredLanguage()), null);
        }
        JsonObject body = content.getAsJsonObject();
        if (!body.has("username") ||
                !body.has("password")) {
            ExceptionReporting.report(PasswordAuthSchemeImpl.class, "authenticating user",
                    "unexpected request body: " + SubmiteeServer.GSON.toJson(content));
            return new AbstractAuthResult(RESULT_BAD_REQUEST,
                    I18N.Http.INVALID_INPUT.format(session.getUser().getPreferredLanguage()), null);
        }

        String sitekey = SubmiteeServer.getInstance().getAttribute("grecaptcha-sitekey", String.class);
        if (sitekey == null || sitekey.isEmpty()) {
            ExceptionReporting.report(PasswordAuthSchemeImpl.class,
                    "missing grecaptcha sitekey", "will not verifying captcha until grecaptcha sitekey is configured");
        } else {
            // region check for captcha
            if (Util.grecaptchaConfigured()) {
                String captcha = body.has("captcha") ? body.get("captcha").getAsString() : null;
                if (captcha == null) {
                    return new AbstractAuthResult(RESULT_REQUIRE_CAPTCHA,
                            I18N.General.REQUIRE_CAPTCHA.format(session.getUser().getPreferredLanguage()), null);
                } else {
                    try {
                        if (!Util.grecaptchaVerify(captcha, session.getLastActiveAddress(), sitekey)) {
                            return new AbstractAuthResult(RESULT_CAPTCHA_FAILURE,
                                    I18N.General.CAPTCHA_FAILURE.format(session.getUser().getPreferredLanguage()), null);
                        }
                    } catch (ClassifiedException e) {
                        ExceptionReporting.report(PasswordAuthSchemeImpl.class, "failed verifying grecaptcha",
                                "failed verifying grecaptcha, skipping", e);
                    }
                }
            }
            // endregion
        }

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        return authHandler.handle(session, username, password);
    }

    @Override
    public String getName() {
        return PasswordAuthScheme.SCHEME_NAME;
    }
}
