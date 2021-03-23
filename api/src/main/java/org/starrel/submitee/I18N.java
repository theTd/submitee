package org.starrel.submitee;

import javax.servlet.ServletContext;

public interface I18N {
    static I18NKey fromKey(String key) {
        return SServer.getInstance().getI18nKey(key);
    }

    enum Auth {
        USER_NOT_EXISTS("auth.user_not_exists");

        private final String key;

        Auth(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        I18NKey getI18NKey() {
            return I18N.fromKey(this.key);
        }
    }

    interface I18NKey {
        String format(String language, Object... parameters);

        String format(ServletContext context, Object... parameters);
    }
}
