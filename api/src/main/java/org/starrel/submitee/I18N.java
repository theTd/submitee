package org.starrel.submitee;

import javax.servlet.http.HttpServletRequest;

public interface I18N {
    static I18NKey fromKey(String key) {
        return SServer.getInstance().getI18nKey(key);
    }

    interface General {
        WrappedI18NKey INTERNAL_ERROR = new WrappedI18NKey("generic.internal_error");
        WrappedI18NKey USER_NOT_EXISTS = new WrappedI18NKey("generic.user_not_exists");
    }

    interface Http {
        WrappedI18NKey INVALID_INPUT = new WrappedI18NKey("http.invalid_input");
        WrappedI18NKey TOO_MANY_REQUEST = new WrappedI18NKey("http.too_many_request");
    }

    interface I18NKey {
        String format(String language, Object... parameters);

        String format(HttpServletRequest httpServletRequest, Object... parameters);
    }

    class WrappedI18NKey implements I18NKey {
        final String key;

        public WrappedI18NKey(String key) {
            this.key = key;
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
}
