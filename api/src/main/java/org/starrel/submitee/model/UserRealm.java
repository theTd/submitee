package org.starrel.submitee.model;

import org.starrel.submitee.auth.AuthScheme;

import java.util.List;

public interface UserRealm {
    String getTypeId();

    User getUser(String id);

    User resumeSession(Session session);

    List<? extends AuthScheme> getSupportedAuthSchemes();

    AuthScheme getAuthScheme(String scheme);

}
