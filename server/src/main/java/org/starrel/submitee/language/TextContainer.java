package org.starrel.submitee.language;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.Util;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import jakarta.servlet.http.HttpServletRequest;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class TextContainer {
    private final Map<String, Map<String, String>> loadedLanguages = new HashMap<>();
    private final Cache<String, I18N.I18NKey> cache = CacheBuilder.newBuilder().build();
    private final Cache<String, String> languageMatchCache = CacheBuilder.newBuilder().build();

    @SuppressWarnings("unchecked")
    private static void flatMap(Map<String, String> container, Map<String, Object> map, String parentNode) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String node = (parentNode == null ? "" : parentNode + ".") + entry.getKey();
            if (entry.getValue() instanceof Map) {
                flatMap(container, (Map<String, Object>) entry.getValue(), node);
            } else {
                container.put(node, entry.getValue() + "");
            }
        }
    }

    private static Map<String, String> flatMap(Map<String, Object> map) {
        Map<String, String> container = new LinkedHashMap<>();
        flatMap(container, map, null);
        return container;
    }

    public void init() throws IOException {
        File directory = new File("text");
        if (directory.isDirectory()) {
            for (File languageFile : Objects.requireNonNull(directory.listFiles((dir, name) -> name.endsWith(".yml")))) {
                String language = languageFile.getName().substring(0, languageFile.getName().length() - 4);
                SubmiteeServer.getInstance().getLogger().info("loading language " + language);
                Map<String, Object> map = new Yaml().load(new FileReader(languageFile));
                loadedLanguages.put(language, flatMap(map));
            }
        }

        if (loadedLanguages.get(SubmiteeServer.getInstance().getDefaultLanguage()) == null) {
            SubmiteeServer.getInstance().getLogger()
                    .warn("unrecognized default language: " + SubmiteeServer.getInstance().getDefaultLanguage());
        }

        try {
            Class.forName("org.starrel.submitee.I18N");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
        updateTemplate(new File("text" + File.separator + "keys.yml"),
                I18N.ConstantI18NKey.KNOWN_KEYS);
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
                if (result.isEmpty()) throw MissingLanguageSignal.INSTANCE;
                return result.get(0);
            });
        } catch (MissingLanguageSignal signal) {
            return null;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof MissingLanguageSignal) {
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    public void updateTemplate(File templateFile, Set<String> knownKeys) throws IOException {
        if (!templateFile.isFile()) {
            if (!templateFile.getParentFile().isDirectory()) {
                if (!templateFile.getParentFile().mkdirs()) {
                    throw new IOException("failed creating template file " + templateFile);
                }
            }

            if (!templateFile.createNewFile())
                throw new IOException("failed creating template file " + templateFile);
        }

        List<String> keyList = new ArrayList<>(knownKeys);
        Collections.sort(keyList);
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : keyList) {
            map.put(key, key);
        }
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        new Yaml(options).dump(map, new PrintWriter(new FileOutputStream(templateFile)));
    }

    private class InternalI18NKey implements I18N.I18NKey {
        private final String key;
        private final Cache<String, String> languageValueCache = CacheBuilder.newBuilder().build();

        private InternalI18NKey(String key) {
            this.key = key;
        }

        private String getValue(String language) {
            String bestMatch = bestMatchLanguage(language);
            Map<String, String> bestMatchTarget = loadedLanguages.get(bestMatch);
            String value;
            if ((value = bestMatchTarget.get(key)) == null) {
                // fall back to secondary match
                for (String secondaryMatch : languageMatchResult(language)) {
                    if (secondaryMatch.equals(bestMatch)) continue;
                    Map<String, String> secondaryMatchTarget = loadedLanguages.get(secondaryMatch);
                    if ((value = secondaryMatchTarget.get(key)) != null) break;
                }
            }

            if (value == null) {
                // fall back to default
                Map<String, String> defaultLanguage = loadedLanguages.get(key);
                value = defaultLanguage == null ? null : defaultLanguage.get(key);
            }
            if (value == null) {
                ExceptionReporting.report(TextContainer.class, "missing language key", "missing language key: " + key);
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

            String message = null;
            try {
                message = String.format(text, parameters);
            } catch (Exception e) {
                ExceptionReporting.report(TextContainer.class, "failed formatting message",
                        String.format("text=%s, params=%s", text, Arrays.deepToString(parameters)));
            }
            if (message == null) message = key;
            return message;
        }

        @Override
        public String format(HttpServletRequest httpServletRequest, Object... parameters) {
            return format(Util.getPreferredLanguage(httpServletRequest), parameters);
        }

        @Override
        public String toString() {
            return format(((String) null));
        }
    }
}
