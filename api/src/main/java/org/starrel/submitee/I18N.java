package org.starrel.submitee;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

public abstract class I18N {
    static {
        try {
            Class.forName("org.starrel.submitee.I18N$General");
            Class.forName("org.starrel.submitee.I18N$Http");
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    public static I18NKey fromKey(String key) {
        return SServer.getInstance().getI18nKey(key);
    }

    public interface I18NKey {
        String format(String language, Object... parameters);

        String format(HttpServletRequest httpServletRequest, Object... parameters);
    }

    public static class ConstantI18NKey implements I18NKey {
        public final static Set<String> KNOWN_KEYS = new HashSet<>();
        private final String key;

        public ConstantI18NKey(String key) {
            this.key = key;
            KNOWN_KEYS.add(key);
        }

        @Override
        public String format(String language, Object... parameters) {
            return SServer.getInstance().getI18nKey(this.key).format(language, parameters);
        }

        @Override
        public String format(HttpServletRequest httpServletRequest, Object... parameters) {
            return SServer.getInstance().getI18nKey(this.key).format(httpServletRequest, parameters);
        }

        @Override
        public String toString() {
            return SServer.getInstance().getI18nKey(this.key).format((String) null);
        }
    }

    public static abstract class General {
        public final static ConstantI18NKey INTERNAL_ERROR = new ConstantI18NKey("generic.internal_error");
        public final static ConstantI18NKey USER_NOT_EXISTS = new ConstantI18NKey("generic.user_not_exists");
    }

    public static abstract class Http {
        public final static ConstantI18NKey INTERNAL_ERROR = new ConstantI18NKey("http.internal_error");
        public final static ConstantI18NKey INVALID_INPUT = new ConstantI18NKey("http.invalid_input");
        public final static ConstantI18NKey TOO_MANY_REQUEST = new ConstantI18NKey("http.too_many_request");
        public final static ConstantI18NKey NOT_FOUND = new ConstantI18NKey("http.not_found");
    }
}
