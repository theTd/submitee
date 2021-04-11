package org.starrel.submitee.auth;

import com.google.gson.JsonElement;
import org.starrel.submitee.model.Session;

import java.util.List;
import java.util.Map;

public interface AuthScheme {

    String getViewUrl(Session session);

    AuthResult auth(Session session, JsonElement content);

    String getName();

    default Map<String, String> getParams(Session session) {
        return null;
    }
}
