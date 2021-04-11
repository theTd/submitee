package org.starrel.submitee.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.User;
import org.starrel.submitee.model.UserDescriptor;
import org.starrel.submitee.model.UserRealm;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AnonymousUserRealm implements UserRealm {
    public final static String TYPE_ID = "anonymous";

    private final Cache<String, AnonymousUser> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES).maximumSize(100).build();

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @SneakyThrows
    @Override
    public User getUser(String userId) {
        return cache.get(userId, () -> new AnonymousUser(userId));
    }

    public User createAnonymousUser(Session session) {
        User user = getUser(session.getSessionToken());
        session.setAttribute("logged-in-user", user.getDescriptor());
        return user;
    }

    @Override
    public User resumeSession(Session session) {
        UserDescriptor loggedInUser = session.getAttribute("logged-in-user", UserDescriptor.class);
        if (loggedInUser != null && Objects.equals(loggedInUser.getRealmType(), TYPE_ID)) {
            return getUser(loggedInUser.getUserId());
        }
        return null;
    }

    @Override
    public List<? extends AuthScheme> getSupportedAuthSchemes() {
        return Collections.emptyList();
    }

    @Override
    public AuthScheme getAuthScheme(String scheme) {
        return null;
    }
}
