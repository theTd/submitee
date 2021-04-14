package org.starrel.submitee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

public class FileLoadingCache {
    private final long updateInterval;

    private final Cache<String, CacheEntry> cache = CacheBuilder.newBuilder().build();

    public FileLoadingCache(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    @SneakyThrows
    public Result getFileContent(String path, String charset) {
        return getFileContent(path, charset, null);
    }

    @SneakyThrows
    public Result getFileContent(String path, String charset, String defaultContent) {
        if (charset == null) {
            charset = "utf-8";
        }
        charset = charset.toLowerCase(Locale.ROOT);
        CacheEntry entry = cache.get(path + ":" + charset,
                () -> new CacheEntry(path, null, -1, -1));

        boolean check = false;
        if (entry.lastCheck == -1 || System.currentTimeMillis() - entry.lastCheck > updateInterval) {
            check = true;
            entry.lastCheck = System.currentTimeMillis();
        }

        Exception exception = null;

        if (check) {
            File file = new File(path);

            try {
                if (file.lastModified() != entry.loadedVersion) {
                    InputStream stream = new FileInputStream(file);
                    ByteArrayOutputStream buff = new ByteArrayOutputStream();
                    ByteStreams.copy(stream, buff);
                    entry.content = buff.toString(charset);
                    entry.loadedVersion = file.lastModified();
                }
            } catch (Exception e) {
                entry.content = defaultContent;
                exception = e;
            }
        }

        return new Result(exception, entry.content, !check);
    }

    @Data
    @AllArgsConstructor
    public static class Result {
        @Getter Exception exception;
        @Getter String content;
        @Getter boolean cached;
    }

    @Data
    @AllArgsConstructor
    public static class CacheEntry {
        @Getter String path;
        @Getter String content;
        @Getter long lastCheck;
        @Getter long loadedVersion;
    }
}
