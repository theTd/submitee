package org.starrel.submitee.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.starrel.submitee.I18N;

public class PasswordAuthSchemeImpl implements PasswordAuthScheme {
    private final static AuthResult RESULT_BAD_REQUEST =
            new AbstractAuthResult("bad_request", I18N.Http.INVALID_INPUT + "", null);
    private final static AuthResult RESULT_INTERNAL_ERROR =
            new AbstractAuthResult("internal_error", I18N.General.INTERNAL_ERROR + "", null);

    private AuthHandler authHandler;

    @Override
    public void setHandler(AuthHandler handler) {
        this.authHandler = handler;
    }

    @Override
    public String getBootstrapPath() {
        // TODO: 2021-03-26-0026
        return null;
    }

    @Override
    public AuthResult auth(JsonElement content) {
        if (authHandler == null) {
            return RESULT_INTERNAL_ERROR;
        }
        if (!content.isJsonObject()) {
            return RESULT_BAD_REQUEST;
        }
        JsonObject body = content.getAsJsonObject();
        if (!body.has("username") ||
                !body.has("password")) {
            return RESULT_BAD_REQUEST;
        }
        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        return authHandler.handle(username, password);
    }

    @Override
    public String getName() {
        return PasswordAuthScheme.SCHEME_NAME;
    }
}
