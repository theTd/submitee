package org.starrel.submitee;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.apache.commons.mail.HtmlEmail;
import org.starrel.submitee.attribute.AttributeSpec;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class Util {
    public static String getRemoteAddr(HttpServletRequest req) {
        String addr = null;
        String header = req.getHeader("X-Forwarded-For");
        if (header != null) {
            int idx = header.indexOf(",");
            addr = idx == -1 ? header : header.substring(0, idx);
        }
        if (addr == null) {
            addr = req.getRemoteAddr();
        }
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

    @SneakyThrows
    public static boolean grecaptchaVerify(String responseToken, String remoteIp, String siteKey) throws ClassifiedException {
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
            throw new ClassifiedException(e, "http_failure", "http failure verifying grecaptcha");
        }

        if (!response.isJsonObject()) {
            throw new ClassifiedException("unexpected response", "expected json object, got: "
                    + SubmiteeServer.GSON.toJson(response));
        }
        return response.getAsJsonObject().get("success").getAsBoolean();
    }

    private final static ExecutorService EMAIL_SENDING_EXECUTOR = Executors.newSingleThreadExecutor();

    public static Future<?> sendNotificationEmail(String recipient, String subject, String message, String abbrev) {
        Preconditions.checkNotNull(recipient);
        Preconditions.checkNotNull(subject);
        Preconditions.checkNotNull(message);

        String path = SubmiteeServer.getInstance().getStaticDirectory() + File.separator
                + "protected" + File.separator + "email-notification.html";

        Map<String, String> map = new LinkedHashMap<>();
        map.put("%TITLE%", subject);
        map.put("%CONTENT%", message);
        map.put("%ABBREV%", abbrev == null ? message : abbrev);
        return sendTemplatedEmail(path, recipient, subject, map, message);
    }

    public static Future<?> sendTemplatedEmail(String path, String recipient, String subject,
                                               Map<String, String> replaces, String plainTextFallback) {
        AttributeSpec<Void> smtp = SubmiteeServer.getInstance().getAttributeMap().of("smtp");
        return EMAIL_SENDING_EXECUTOR.submit((Callable<Void>) () -> {
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
            email.setHostName(smtp.get("server", String.class));
            email.setAuthentication(smtp.get("user", String.class), smtp.get("password", String.class));
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
}
