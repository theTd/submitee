package org.starrel.submitee.model;

import org.starrel.submitee.SServer;
import org.starrel.submitee.attribute.AttributeMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.Date;

public class SessionImpl implements Session {
    public final static String HTTP_SESSION_ATTRIBUTE_KEY = "session";
    public final static String TOKEN_COOKIE_KEY = "submitee_token";
    private final SServer server;
    private final HttpSession httpSession;
    private final AttributeMap<Session> attributeMap;

    private boolean anonymous;
    private User user;

    public SessionImpl(SServer server, HttpSession httpSession) {
        this(server, httpSession, null, null);
    }

    public SessionImpl(SServer server, HttpSession httpSession, User user, String sessionToken) {
        this.server = server;
        this.httpSession = httpSession;
        this.user = user;
        this.anonymous = this.user == null;
        if (anonymous || sessionToken == null) {
            this.attributeMap = server.createAttributeMap(this);
        } else {
            this.attributeMap = server.readAttributeMap(this, Session.ATTRIBUTE_COLLECTION_NAME, sessionToken);
        }
    }

    public static SessionImpl getSession(HttpSession httpSession) {
        Object raw = httpSession.getAttribute(HTTP_SESSION_ATTRIBUTE_KEY);
        return raw == null ? null : (SessionImpl) raw;
    }

    public static SessionImpl createAnonymous(SServer server, HttpSession httpSession) {
        SessionImpl instance = new SessionImpl(server, httpSession);
        httpSession.setAttribute(HTTP_SESSION_ATTRIBUTE_KEY, instance);
        return instance;
    }

    public static SessionImpl create(SServer server, HttpSession httpSession, User user) {
        SessionImpl instance = new SessionImpl(server, httpSession, user, null);
        httpSession.setAttribute(HTTP_SESSION_ATTRIBUTE_KEY, instance);
        return instance;
    }

    public static SessionImpl fromCookie(Cookie[] cookies) {
        String token = null;
        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE_KEY.equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }
        if (token == null || token.isEmpty()) return null;
        return fromToken(token);
    }

    private static SessionImpl fromToken(String token) {
        // TODO: 2021/3/23 query token
        return null;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
        this.anonymous = this.user == null;
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public String getLastUA() {
        return null;
    }

    @Override
    public Date getLastActive() {
        return null;
    }

    @Override
    public String getAttributePersistKey() {
        return null;
    }

    @Override
    public AttributeMap<Session> getAttributeMap() {
        return null;
    }

    @Override
    public void attributeUpdated(String path) {
    }

    @Override
    public void close() {
        httpSession.setAttribute(HTTP_SESSION_ATTRIBUTE_KEY, null);
        // TODO: 2021/3/23 invalidate token
    }
}
