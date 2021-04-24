package org.starrel.submitee;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.gson.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.apache.commons.mail.HtmlEmail;
import org.eclipse.jetty.http.HttpStatus;
import org.jsoup.Jsoup;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.starrel.submitee.attribute.AttributeSpec;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public abstract class Util {
    public static String getRemoteAddr(HttpServletRequest req) {
        String set = (String) req.getAttribute("REMOTE-ADDR");
        if (set != null) return set;

        String addr = null;
        String header = req.getHeader("X-Forwarded-For");
        if (header != null) {
            int idx = header.indexOf(",");
            addr = idx == -1 ? header : header.substring(0, idx);
        }
        if (addr == null) {
            addr = req.getRemoteAddr();
        }
        req.setAttribute("REMOTE-ADDR", addr);
        return addr;
    }

    public static String getPreferredLanguage(HttpServletRequest req) {
        String header = req.getHeader("Accept-Language");
        String language;
        if (header == null) {
            language = SubmiteeServer.getInstance().getDefaultLanguage();
        } else {
            language = header.split(",")[0];
        }
        return language;
    }

    public static boolean grecaptchaConfigured() {
        String grecaptchaSitekey = SubmiteeServer.getInstance().getAttribute("grecaptcha-sitekey", String.class);
        String grecaptchaSecretKey = SubmiteeServer.getInstance().getAttribute("grecaptcha-secretkey", String.class);
        return grecaptchaSitekey != null && grecaptchaSecretKey != null &&
                !grecaptchaSitekey.isEmpty() && !grecaptchaSecretKey.isEmpty();
    }

    public static boolean grecaptchaVerify(String responseToken, String remoteIp, String siteKey) throws ClassifiedException {
        // TODO: 2021-04-12-0012 add remote ip into request
        byte[] body = String.format("secret=%s&response=%s", siteKey, responseToken).getBytes(StandardCharsets.UTF_8);

        JsonElement response;
        try {
            URL url = new URL("https://www.recaptcha.net/recaptcha/api/siteverify");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            ByteStreams.copy(new ByteArrayInputStream(body), conn.getOutputStream());
            InputStreamReader responseReader = new InputStreamReader(conn.getInputStream());
            response = JsonParser.parseReader(responseReader);
        } catch (Exception e) {
            throw new ClassifiedException(ClassifiedErrors.INTERNAL_SERVER_ERROR);
        }

        JsonArray errorCodes = JsonUtil.parseArray(response, "error-codes");
        if (errorCodes != null) {
            throw new ClassifiedException("GRECAPTCHA_ERROR_CODES", HttpStatus.INTERNAL_SERVER_ERROR_500,
                    SubmiteeServer.GSON.toJson(errorCodes));
        }

        Boolean success = JsonUtil.parseBoolean(response, "success");
        if (success == null) {
            ExceptionReporting.report(Util.class, "parsing grecaptcha response", "unexpected response: " +
                    SubmiteeServer.GSON.toJson(response));
            throw new ClassifiedException(ClassifiedErrors.INTERNAL_SERVER_ERROR);
        }

        return success;
    }

    private final static ExecutorService EMAIL_SENDING_EXECUTOR = Executors.newSingleThreadExecutor();

    public static Future<?> sendNotificationEmail(String recipient, String subject, String message, String abbrev) {
        Preconditions.checkNotNull(recipient);
        Preconditions.checkNotNull(subject);
        Preconditions.checkNotNull(message);

        String path = SubmiteeServer.getStaticDirectory()
                + "protected" + File.separator + "email-notification.html";

        Map<String, String> map = new LinkedHashMap<>();
        map.put("%TITLE%", subject);
        map.put("%CONTENT%", message);
        map.put("%ABBREV%", abbrev == null ? message : abbrev);
        return sendTemplatedEmail(path, recipient, subject, map, message);
    }

    public static Future<?> sendVerifyCodeEmail(String recipient, String verifyCode, String preferredLanguage) {
        String path = SubmiteeServer.getStaticDirectory()
                + "protected" + File.separator + "email-verify-code.html";

        String subject = I18N.Email.EMAIL_SUBJECT_VERIFY_CODE.format(preferredLanguage, verifyCode);
        return sendTemplatedEmail(path, recipient, subject,
                Collections.singletonMap("%CODE%", verifyCode), subject);
    }

    public static Future<?> sendTemplatedEmail(String path, String recipient, String subject,
                                               Map<String, String> replaces, String plainTextFallback) {
        AttributeSpec<Void> smtp = SubmiteeServer.getInstance().getAttributeMap().of("smtp");
        return EMAIL_SENDING_EXECUTOR.submit((Callable<Void>) () -> {
            String smtpHostname = smtp.get("server", String.class);
            String smtpUser = smtp.get("user", String.class);
            String smtpPassword = smtp.get("password", String.class);

            if (smtpHostname == null || smtpHostname.isEmpty()) {
                throw new RuntimeException("empty smtp hostname");
            }

            FileLoadingCache.Result result = SubmiteeServer.getInstance().getFileLoadingCache()
                    .getFileContent(path, "utf-8");
            if (result.exception != null) {
                throw result.exception;
            }
            String content = result.content;
            for (Map.Entry<String, String> entry : replaces.entrySet()) {
                content = content.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
            }

            // Create the email message
            HtmlEmail email = new HtmlEmail();
            email.setHostName(smtpHostname);
            email.setAuthentication(smtpUser, smtpPassword);
            if (smtp.get("ssl", Boolean.class, true)) {
                email.setSSLOnConnect(true);
                email.setSslSmtpPort(smtp.get("port", Integer.class) + "");
            } else {
                email.setSSLOnConnect(false);
                email.setSmtpPort(smtp.get("port", Integer.class));
            }
            email.addTo(recipient);
            String senderAddress = smtp.get("sender-address", String.class);
            if (senderAddress == null || senderAddress.isEmpty()) senderAddress = smtp.get("user", String.class);
            String senderName = smtp.get("sender-name", String.class);
            if (senderName == null || senderName.isEmpty()) senderName = "SUBMITEE";
            email.setFrom(senderAddress, senderName);
            email.setSubject(subject);

//            // embed the image and get the content id
//            URL url = new URL("http://www.apache.org/images/asf_logo_wide.gif");
//            String cid = email.embed(url, "Apache logo");
            email.setHtmlMsg(content);
            email.setTextMsg(plainTextFallback);
            email.setCharset("utf-8");
            email.updateContentType("text/html");
            email.send();
            return null;
        });
    }

    private final static PolicyFactory POLICY;

    static {
        POLICY = new HtmlPolicyBuilder()
                .allowElements("font")
                .allowAttributes("color").onElements("font")
                .allowAttributes("face").onElements("font")
                .allowElements("pre")
                .allowElements("code")
                .allowAttributes("class").onElements("code")
                .allowElements("xmp")
                .toFactory().and(Sanitizers.FORMATTING).and(Sanitizers.LINKS)
                .and(Sanitizers.BLOCKS).and(Sanitizers.STYLES).and(Sanitizers.TABLES).and(Sanitizers.IMAGES);
    }

    public static String sanitizeHtml(String untrustedHtml) {
        return POLICY.sanitize(untrustedHtml);
    }

    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> T sanitizeJson(T element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (String key : new ArrayList<>(obj.keySet())) {
                obj.add(key, sanitizeJson(obj.get(key)));
            }
            return (T) obj;
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, sanitizeJson(arr.get(i)));
            }
            return (T) arr;
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                String str = primitive.getAsString();
                if (str.contains("<")) {
                    return (T) new JsonPrimitive(sanitizeHtml(str));
                }
            }
            return (T) primitive;
        } else {
            return element;
        }
    }

    public static boolean isEmptyHtml(String html) {
        if (html == null || html.isEmpty()) return true;
        String text;
        try {
            text = Jsoup.parse(html).text();
        } catch (Exception e) {
            ExceptionReporting.report(Util.class, "parsing html", "html=" + html);
            return true;
        }
        if (text == null) return true;
        text = text.replace(" ", "");
        return text.isEmpty();
    }
}
