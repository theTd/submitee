package org.starrel.submitee.model;

import java.util.List;

public interface UserRealm {
    String getTypeId();

    User getUser(String id);

    default User getAnonymousUser() {
        throw new UnsupportedOperationException("anonymous user is not supported");
    }

    List<Class<? extends AuthScheme>> getSupportedAuthSchemes();

}
