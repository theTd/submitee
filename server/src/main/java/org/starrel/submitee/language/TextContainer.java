package org.starrel.submitee.language;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class TextContainer {
    private final Map<String, Properties> loadedLanguages = new HashMap<>();
    private final Cache<String, I18N.I18NKey> cache = CacheBuilder.newBuilder().build();
    private final Cache<String, String> languageMatchCache = CacheBuilder.newBuilder().build();
    private Properties defaultLanguage;

    public void init() throws IOException {
        File directory = new File("text");
        if (directory.isDirectory()) {
            for (File languageFile : Objects.requireNonNull(directory.listFiles((dir, name) -> name.endsWith(".properties")))) {
                Properties props = new Properties();
                props.load(new FileInputStream(languageFile));
                String language = languageFile.getName().substring(0, languageFile.getName().length() - 11);

                SubmiteeServer.getInstance().getLogger().info("loading language " + language);
                loadedLanguages.put(language, props);
            }
        }

        defaultLanguage = loadedLanguages.get(SubmiteeServer.getInstance().getDefaultLanguage());
        if (defaultLanguage == null) {
            SubmiteeServer.getInstance().getLogger()
                    .warn("unrecognized default language: " + SubmiteeServer.getInstance().getDefaultLanguage());
        }
    }

    public I18N.I18NKey get(String key) {
        try {
            return cache.get(key, () -> new InternalI18NKey(key));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> languageMatchResult(String language) {
        LanguageMatcher matcher = new LanguageMatcher(loadedLanguages);
        Iterator<String> pathIterator = Arrays.stream(language.split("-")).iterator();
        while (pathIterator.hasNext()) {
            String path = pathIterator.next();
            matcher.next(path);
        }
        return matcher.end();
    }

    private String bestMatchLanguage(String language) {
        try {
            return languageMatchCache.get(language, () -> {
                List<String> result = languageMatchResult(language);
                if (result.isEmpty()) return null;
                return result.get(0);
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTemplate(File templateFile, Set<String> knownKeys) throws IOException {
        Properties properties = new Properties();
        List<String> keyList = new ArrayList<>(knownKeys);
        Collections.sort(keyList);
        for (String key : keyList) {
            properties.put(key, key);
        }
        properties.store(new PrintWriter(new FileOutputStream(templateFile)),
                "auto-generated template language file");
    }

    private class InternalI18NKey implements I18N.I18NKey {
        private final String key;
        private final Cache<String, String> languageValueCache = CacheBuilder.newBuilder().build();

        private InternalI18NKey(String key) {
            this.key = key;
        }

        private String getValue(String language) {
            String bestMatch = bestMatchLanguage(language);
            Properties bestMatchTarget = loadedLanguages.get(bestMatch);
            String value;
            if ((value = bestMatchTarget.getProperty(key)) == null) {
                // fall back to secondary match
                for (String secondaryMatch : languageMatchResult(language)) {
                    if (secondaryMatch.equals(bestMatch)) continue;
                    Properties secondaryMatchTarget = loadedLanguages.get(secondaryMatch);
                    if ((value = secondaryMatchTarget.getProperty(key)) == null) continue;
                }
            }

            if (value == null) {
                // fall back to default
                value = defaultLanguage.getProperty(key);
            }
            if (value == null) {
                ExceptionReporting.report("could not find any value of language key: " + key);
                return key;
            }
            return value;
        }

        @Override
        public String format(String language, Object... parameters) {
            if (language == null) language = SubmiteeServer.getInstance().getDefaultLanguage();
            String text;
            try {
                String finalLanguage = language;
                text = languageValueCache.get(language, () -> getValue(finalLanguage));
                if (text == null) return key;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

            return String.format(text, parameters);
        }

        @Override
        public String format(HttpServletRequest httpServletRequest, Object... parameters) {
            String header = httpServletRequest.getHeader("Accept-Language");
            String language;
            if (header == null) {
                language = SubmiteeServer.getInstance().getDefaultLanguage();
            } else {
                language = header.split(",")[0];
            }
            return format(language, parameters);
        }
    }

}
