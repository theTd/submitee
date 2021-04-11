package org.starrel.submitee.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import org.starrel.submitee.*;
import org.starrel.submitee.model.Session;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PasswordAuthSchemeImpl implements PasswordAuthScheme {
    private final static String RESULT_BAD_REQUEST = "bad_request";
    private final static String RESULT_INTERNAL_ERROR = "internal_error";
    private final static String RESULT_REQUIRE_CAPTCHA = "require_captcha";
    private final static String RESULT_CAPTCHA_FAILURE = "captcha_failure";

    private final Cache<String, Integer> failureCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS).maximumSize(1000).build();

    private Map<String, String> params = null;

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
        if (requireCaptcha(session)) {
            String grecaptchaSitekey = SubmiteeServer.getInstance().getAttribute("grecaptcha-sitekey", String.class);
            String grecaptchaSecretKey = SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class);
            if (grecaptchaSitekey != null && grecaptchaSecretKey != null) {
                if (params == null) {
                    params = new LinkedHashMap<>();
                    params.put("g", grecaptchaSitekey);
                }
                return params;
            }
        }
        return null;
    }

    @SneakyThrows
    private boolean requireCaptcha(Session session) {
        Integer failure = failureCache.get(session.getLastActiveAddress(), () -> 0);
        return failure > 5;
    }

    @SneakyThrows
    private void markAuthFailure(Session session) {
        Integer failure = failureCache.get(session.getLastActiveAddress(), () -> 0);
        failureCache.put(session.getLastActiveAddress(), ++failure);
    }

    private void clearAuthFailure(Session session) {
        failureCache.invalidate(session.getLastActiveAddress());
    }

    @Override
    public AuthResult auth(Session session, JsonElement content) {
        if (authHandler == null) {
            markAuthFailure(session);
            ExceptionReporting.report(PasswordAuthSchemeImpl.class, "authenticating user", "auth handler not set");
            return new AbstractAuthResult(RESULT_INTERNAL_ERROR,
                    I18N.Http.INTERNAL_ERROR.format(session.getUser().getPreferredLanguage()), null);
        }

        if (!content.isJsonObject()) {
            markAuthFailure(session);
            ExceptionReporting.report(PasswordAuthSchemeImpl.class, "authenticating user",
                    "unexpected request body: " + SubmiteeServer.GSON.toJson(content));
            return new AbstractAuthResult(RESULT_BAD_REQUEST,
                    I18N.Http.INVALID_INPUT.format(session.getUser().getPreferredLanguage()), null);
        }
        JsonObject body = content.getAsJsonObject();
        if (!body.has("username") ||
                !body.has("password")) {
            markAuthFailure(session);
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
            if (requireCaptcha(session)) {
                String captcha = body.has("captcha") ? body.get("captcha").getAsString() : null;
                if (captcha == null) {
                    markAuthFailure(session);
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
        AuthResult result = authHandler.handle(session, username, password);
        if (!result.isAccepted()) {
            markAuthFailure(session);
        } else {
            clearAuthFailure(session);
        }
        return result;
    }

    @Override
    public String getName() {
        return PasswordAuthScheme.SCHEME_NAME;
    }
}
