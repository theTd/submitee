package org.starrel.submitee.model;

import org.starrel.submitee.SServer;
import org.starrel.submitee.attribute.AttributeMap;

import javax.servlet.http.HttpSession;

public class SessionImpl implements Session {
    public final static String ATT_KEY = "session";
    private final HttpSession httpSession;
    private final AttributeMap<Session> attributeMap;

    private boolean anonymous;
    private User user;

    public SessionImpl(SServer server, HttpSession httpSession) {
        this.httpSession = httpSession;
        this.user = null;
        anonymous = true;
        attributeMap = server.createAttributeMap(this);
    }

    public SessionImpl(SServer server, HttpSession httpSession, User user) {
        this.httpSession = httpSession;
        this.user = user;
        anonymous = false;
        attributeMap = server.createAttributeMap(this);
    }

    public static SessionImpl getSession(HttpSession httpSession) {
        Object raw = httpSession.getAttribute(ATT_KEY);
        return raw == null ? null : (SessionImpl) raw;
    }

    public static SessionImpl createAnonymous(SServer server, HttpSession httpSession) {
        SessionImpl instance = new SessionImpl(server, httpSession);
        httpSession.setAttribute(ATT_KEY, instance);
        return instance;
    }

    public static SessionImpl create(SServer server, HttpSession httpSession, User user) {
        SessionImpl instance = new SessionImpl(server, httpSession, user);
        httpSession.setAttribute(ATT_KEY, instance);
        return instance;
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
        httpSession.setAttribute(ATT_KEY, null);
    }
}
