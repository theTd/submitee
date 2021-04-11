package org.starrel.submitee;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.apache.commons.mail.HtmlEmail;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

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

    public static Future<?> sendTemplatedEmail(String path, String recipient, String smtp, String from,
                                               String fromName, String subject, Map<String, String> replaces) {
        return EMAIL_SENDING_EXECUTOR.submit((Callable<Void>) () -> {
            // Create the email message
            HtmlEmail email = new HtmlEmail();
            email.setHostName(smtp);
            email.addTo(recipient);
            email.setFrom(from, fromName);
            email.setSubject(subject);

//            // embed the image and get the content id
//            URL url = new URL("http://www.apache.org/images/asf_logo_wide.gif");
//            String cid = email.embed(url, "Apache logo");
//
//            // set the html message
//            email.setHtmlMsg("<html>The apache logo - <img src=\"cid:" + cid + "\"></html>");
//
//            // set the alternative message
//            email.setTextMsg("Your email client does not support HTML messages");

            // TODO: 2021-04-12-0012
            email.setHtmlMsg(null);
            email.send();
            return null;
        });
    }
}
