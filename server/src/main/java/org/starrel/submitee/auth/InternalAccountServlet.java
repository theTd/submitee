package org.starrel.submitee.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.eclipse.jetty.http.HttpStatus;
import org.starrel.submitee.*;
import org.starrel.submitee.http.AbstractJsonServlet;
import org.starrel.submitee.model.User;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class InternalAccountServlet extends AbstractJsonServlet {
    private final static int VERIFY_CODE_TIMEOUT = 1000 * 30;

    {
        setBaseUri("/internal-account");
    }

    private final LoadingCache<String, String> verifyCodeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                private final Random random = new Random();

                @Override
                public String load(String s) throws Exception {
                    return (100000 + random.nextInt(899999)) + "";
                }
            });

    private final Cache<String, TimeThrottleList> addrVerifyCodeCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS).build();
    private final Cache<String, TimeThrottleList> sessionVerifyCodeCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES).build();

    @SneakyThrows
    private boolean checkVerifyCodeSending(HttpServletRequest req) {
        TimeThrottleList list = addrVerifyCodeCache.get(Util.getRemoteAddr(req),
                () -> new TimeThrottleList(1000 * 60, 50));
        if (!list.checkViolation()) return false;
        list = sessionVerifyCodeCache.get(getSession(req).getSessionToken(),
                () -> new TimeThrottleList(VERIFY_CODE_TIMEOUT - 1000, 1));
        return list.checkViolation();
    }

    private long getVerifyCodeTimeout(HttpServletRequest req) {
        TimeThrottleList list = sessionVerifyCodeCache.getIfPresent(getSession(req).getSessionToken());
        if (list == null) return 0;
        return list.getNewest() + VERIFY_CODE_TIMEOUT;
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
        if (Util.grecaptchaConfigured()) {
            writer.name("grecaptcha-sitekey").value(
                    SubmiteeServer.getInstance().getAttribute("grecaptcha-sitekey", String.class));
        }
        long verifyCodeTimeout = getVerifyCodeTimeout(req);
        if (verifyCodeTimeout > 0) {
            writer.name("verify-code-timeout").value(verifyCodeTimeout);
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
                String resetPassword = body.has("reset-password") ? body.get("reset-password").getAsString() : null;
                String token = null;
                if (grecaptcha) {
                    token = body.has("captcha") ? body.get("captcha").getAsString() : null;
                }
                if (email == null && resetPassword == null) {
                    responseBadRequest(req, resp);
                    return;
                }
                if (email != null && resetPassword != null) {
                    responseBadRequest(req, resp);
                    return;
                }
                if (grecaptcha && token == null) {
                    responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.REQUIRE_CAPTCHA.format(req));
                    return;
                }
                String finalToken = token;
                AsyncContext asyncContext = req.startAsync();
                asyncContext.start(() -> {
                    try {
                        if (grecaptcha) {
                            if (!Util.grecaptchaVerify(finalToken, Util.getRemoteAddr(req),
                                    SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class))) {
                                responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.CAPTCHA_FAILURE.format(req));
                                return;
                            }
                        }

                        String actualEmail = email;
                        if (email != null) {
                            Integer uid = SubmiteeServer.getInstance().getInternalAccountRealm().getUidFromEmail(email);
                            if (uid != null) {
                                responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.USER_EXISTS_EMAIL.format(req, email));
                                return;
                            }
                        } else {
                            // search for email or username
                            InternalAccountUser found = SubmiteeServer.getInstance().getInternalAccountRealm()
                                    .getUserFromUsernameOrEmail(resetPassword);
                            if (found == null) {
                                responseErrorPage(resp, HttpStatus.NOT_FOUND_404, I18N.General.USER_NOT_EXISTS.format(req));
                                return;
                            } else {
                                actualEmail = found.getEmail();
                            }
                        }

                        if (!checkVerifyCodeSending(req)) {
                            responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.Http.TOO_MANY_REQUEST.format(req));
                            return;
                        }

                        verifyCodeCache.invalidate(actualEmail.toLowerCase(Locale.ROOT));
                        String gen = verifyCodeCache.get(actualEmail.toLowerCase(Locale.ROOT));
                        Util.sendVerifyCodeEmail(actualEmail, gen, Util.getPreferredLanguage(req)).get();
                        resp.setStatus(HttpStatus.OK_200);
                        resp.setContentType("application/json");
                        resp.getWriter().println(SubmiteeServer.GSON.toJson(getVerifyCodeTimeout(req)));
                    } catch (Exception e) {
                        ExceptionReporting.report(InternalAccountServlet.class, "processing send-verify-code", e);
                        try {
                            responseInternalError(req, resp);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    } finally {
                        asyncContext.complete();
                    }
                });
                break;
            }
            case "register": {
                String email = body.has("email") ? body.get("email").getAsString() : null;
                String password = body.has("password") ? body.get("password").getAsString() : null;
                String captcha = body.has("captcha") ? body.get("captcha").getAsString() : null;
                String verifyCode = body.has("verify-code") ? body.get("verify-code").getAsString() : null;

                if (email == null || email.isEmpty() || password == null || password.isEmpty() || verifyCode == null) {
                    responseBadRequest(req, resp);
                    return;
                }
                if (Util.grecaptchaConfigured() && captcha == null) {
                    responseErrorPage(resp, HttpStatus.BAD_REQUEST_400, I18N.General.REQUIRE_CAPTCHA.format(req));
                    return;
                }
                if (!Objects.equals(verifyCodeCache.getIfPresent(email.toLowerCase(Locale.ROOT)), verifyCode)) {
                    responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.VERIFY_CODE_MISMATCH.format(req));
                    return;
                }
                verifyCodeCache.invalidate(email.toLowerCase(Locale.ROOT));
                AsyncContext asyncContext = req.startAsync();
                asyncContext.start(() -> {
                    try {
                        if (Util.grecaptchaConfigured()) {
                            if (!Util.grecaptchaVerify(captcha, Util.getRemoteAddr(req),
                                    SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class))) {
                                responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.CAPTCHA_FAILURE.format(req));
                                return;
                            }
                        }

                        Integer uid = SubmiteeServer.getInstance().getInternalAccountRealm().getUidFromEmail(email);
                        if (uid != null) {
                            responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.USER_EXISTS_EMAIL.format(req, email));
                            return;
                        }

                        User created = SubmiteeServer.getInstance().getInternalAccountRealm().createUser(email, password);
                        created.setAttribute("create-time", System.currentTimeMillis());
                        created.setAttribute("last-seen", System.currentTimeMillis());
                        created.setAttribute("preferred-language", Util.getPreferredLanguage(req));

                        getSession(req).setUser(created);
                        resp.setStatus(HttpStatus.OK_200);
                    } catch (Exception e) {
                        ExceptionReporting.report(InternalAccountServlet.class, "creating user", e);
                        try {
                            responseInternalError(req, resp);
                        } catch (IOException ioException) {
                            throw new RuntimeException(ioException);
                        }
                    } finally {
                        asyncContext.complete();
                    }
                });
                break;
            }
            case "reset-password": {
                String resetPassword = body.has("reset-password") ? body.get("reset-password").getAsString() : null;
                String captcha = body.has("captcha") ? body.get("captcha").getAsString() : null;
                String verifyCode = body.has("verify-code") ? body.get("verify-code").getAsString() : null;
                String newPassword = body.has("new-password") ? body.get("new-password").getAsString() : null;

                if (resetPassword == null || verifyCode == null || newPassword == null || newPassword.isEmpty()) {
                    responseBadRequest(req, resp);
                    return;
                }
                if (Util.grecaptchaConfigured() && captcha == null) {
                    responseErrorPage(resp, HttpStatus.BAD_REQUEST_400, I18N.General.REQUIRE_CAPTCHA.format(req));
                    return;
                }
                AsyncContext asyncContext = req.startAsync();
                asyncContext.start(() -> {
                    try {
                        if (Util.grecaptchaConfigured()) {
                            if (!Util.grecaptchaVerify(captcha, Util.getRemoteAddr(req),
                                    SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class))) {
                                responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.CAPTCHA_FAILURE.format(req));
                                return;
                            }
                        }

                        // search for email or username
                        InternalAccountUser found = SubmiteeServer.getInstance().getInternalAccountRealm()
                                .getUserFromUsernameOrEmail(resetPassword);
                        if (found == null) {
                            responseErrorPage(resp, HttpStatus.NOT_FOUND_404, I18N.General.USER_NOT_EXISTS.format(req));
                            return;
                        }
                        String email = found.getEmail();
                        if (!Objects.equals(verifyCodeCache.getIfPresent(email), verifyCode)) {
                            responseErrorPage(resp, HttpStatus.FORBIDDEN_403, I18N.General.VERIFY_CODE_MISMATCH.format(req));
                            return;
                        }
                        verifyCodeCache.invalidate(email);

                        found.setPassword(newPassword);
                        resp.setStatus(HttpStatus.OK_200);
                    } catch (Exception e) {
                        ExceptionReporting.report(InternalAccountServlet.class, "creating user", e);
                        try {
                            responseInternalError(req, resp);
                        } catch (IOException ioException) {
                            throw new RuntimeException(ioException);
                        }
                    } finally {
                        asyncContext.complete();
                    }
                });
                break;
            }
            default: {
                ExceptionReporting.report(InternalAccountServlet.class, "parsing method", "unknown method: " + uriParts[0]);
                responseBadRequest(req, resp);
            }
        }
    }
}
