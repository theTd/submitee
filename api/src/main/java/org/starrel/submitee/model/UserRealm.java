package org.starrel.submitee.model;

import org.starrel.submitee.auth.AuthScheme;

import java.util.List;

public interface UserRealm {
    String getTypeId();

    User getUser(String id);

    default User getAnonymousUser() {
        throw new UnsupportedOperationException("anonymous user is not supported");
    }

    List<? extends AuthScheme> getSupportedAuthSchemes();

}
