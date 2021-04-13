package org.starrel.submitee.auth;

import org.starrel.submitee.model.Session;

public interface PasswordAuthScheme extends AuthScheme {
    String SCHEME_NAME = "password";

    void setHandler(AuthHandler handler);

    interface AuthHandler {
        AuthResult handle(Session session, String username, String password);

        String getResetPasswordLink();
    }
}
