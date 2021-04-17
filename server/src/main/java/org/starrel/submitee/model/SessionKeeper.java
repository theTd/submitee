package org.starrel.submitee.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.bson.Document;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.Util;
import org.starrel.submitee.auth.AnonymousUser;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SessionKeeper {
    public final static String HTTP_ATTRIBUTE_SESSION = "submitee_session";
    public final static String COOKIE_NAME_SESSION_TOKEN = "sess";
    private final Cache<String, SessionImpl> cache = CacheBuilder.newBuilder().weakValues().build();
    private final Cache<UserDescriptor, String> userCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES).maximumSize(1000).build();

    private static String generateNewSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String getSessionTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (Objects.equals(cookie.getName(), COOKIE_NAME_SESSION_TOKEN)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public SessionImpl resumeFromHttpRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = getSessionTokenFromCookies(cookies);
        SessionImpl sess = token == null ? null : getByToken(token);
        if (sess == null) {
            token = generateNewSessionToken();
            sess = new SessionImpl(this, token);
            cache.put(token, sess);
        }
        sess.getAttributeMap().setAutoSaveAttribute(false);

        User resumedUser = SubmiteeServer.getInstance().resumeSession(sess);
        if (resumedUser == null) {
            sess.setUser(SubmiteeServer.getInstance().getAnonymousUserRealm().createAnonymousUser(sess));
            sess.getUser().setPreferredLanguage(Util.getPreferredLanguage(request));
            sess.setAttribute("logged-in-user", sess.getUser().getDescriptor());
        } else {
            sess.setUser(resumedUser);
            SubmiteeServer.getInstance().pushEvent(Level.INFO, SessionKeeper.class.getName(), "session resumed",
                    String.format("user=%s, session=%s, addr=%s", resumedUser.getDescriptor().toString(),
                            sess.getSessionToken(), Util.getRemoteAddr(request)));
        }

        sess.setLastUA(request.getHeader("User-Agent"));
        sess.pushLastActive(request);

        sess.getAttributeMap().setAutoSaveAttribute(!sess.getUser().isAnonymous());
        return sess;
    }

    @SneakyThrows
    public SessionImpl getByToken(String token) {
        try {
            return cache.get(token, () -> {
                if (SubmiteeServer.getInstance().attributeMapExist(token, Session.COLLECTION_NAME)) {
                    SessionImpl sess = new SessionImpl(this, token);
                    sess.getAttributeMap().read();
                    return sess;
                }
                throw NotExistsSignal.INSTANCE;
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NotExistsSignal) {
                return null;
            }
            throw e;
        }
    }

    public SessionImpl getByUser(UserDescriptor userDescriptor) throws ExecutionException {
        String token;
        try {
            token = userCache.get(userDescriptor, () -> {
                MongoDatabase mongoDatabase = SubmiteeServer.getInstance().getMongoDatabase();
                Document found = mongoDatabase.getCollection(Session.COLLECTION_NAME)
                        .find(Filters.eq("logged-in-user", userDescriptor.toString()))
                        .projection(Projections.include("id"))
                        .first();
                if (found == null) throw NotExistsSignal.INSTANCE;
                return found.getString("id");
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NotExistsSignal) {
                token = null;
            } else {
                throw e;
            }
        }
        if (token == null) return null;
        return getByToken(token);
    }

    void remove(SessionImpl session) {
        cache.invalidate(session.getSessionToken());
        userCache.asMap().entrySet().stream()
                .filter(en -> Objects.equals(en.getValue(), session.getSessionToken())).findFirst()
                .ifPresent(en -> userCache.invalidate(en.getKey()));
    }
}
