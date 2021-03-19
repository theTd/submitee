package org.starrel.submitee.auth;

import org.starrel.submitee.model.AuthScheme;
import org.starrel.submitee.model.User;
import org.starrel.submitee.model.UserRealm;

import java.util.List;

public class InternalAccountRealm implements UserRealm {
    public final static String TYPE_ID = "internal";
    private final static InternalAccountUser ANONYMOUS = new InternalAccountUser(null);

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public User getUser(String id) {
        // TODO: 2021-03-18-0018
        return null;
    }

    @Override
    public User getAnonymousUser() {
        return null;
    }

    @Override
    public List<Class<? extends AuthScheme>> getSupportedAuthSchemes() {
        return null;
    }
}
