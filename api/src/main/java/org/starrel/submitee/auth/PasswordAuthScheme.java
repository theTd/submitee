package org.starrel.submitee.auth;

public interface PasswordAuthScheme extends AuthScheme {

    void setHandler(AuthHandler handler);

    interface AuthHandler {
        AuthResult handle(String username, String password);
    }
}
