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

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

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

    @SneakyThrows
    public static boolean gRecaptchaVerify(String responseToken, String remoteIp, String siteKey) throws ClassifiedException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter);
        writer.beginObject();
        writer.name("secret").value(siteKey);
        writer.name("response").value(responseToken);
        writer.name("remoteip").value(remoteIp);
        writer.endObject();
        byte[] body = stringWriter.toString().getBytes(StandardCharsets.UTF_8);

        JsonElement response;
        try {
            URL url = new URL("https://www.recaptcha.net/recaptcha/api/siteverify");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.setRequestProperty("Content-Length", body.length + "");
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
}
