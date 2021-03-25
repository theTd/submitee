package org.starrel.submitee.auth;

import com.google.gson.JsonElement;

public interface AuthScheme {

    String getBootstrapPath();

    AuthResult auth(JsonElement content);

    String getName();
}
