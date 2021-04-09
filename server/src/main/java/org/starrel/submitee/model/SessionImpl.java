package org.starrel.submitee.model;

import jakarta.servlet.http.HttpServletRequest;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.Util;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;
import org.starrel.submitee.auth.AnonymousUser;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SessionImpl implements Session {
    public final static String HTTP_ATTRIBUTE_SESSION = "submitee_session";
    public final static String COOKIE_NAME_SESSION_TOKEN = "sess";

    private final HttpSession httpSession;
    private final String sessionToken;

    private final AttributeMap<SessionImpl> attributeMap;

    private final AttributeSpec<String> lastUA;
    private final AttributeSpec<Date> lastActive;
    private final AttributeSpec<String> lastActiveAddress;
    private final AttributeSpec<HistoryAddressEntry> historyAddress;

    private User user;

    private SessionImpl(HttpSession httpSession, Cookie[] cookies) {
        this.httpSession = httpSession;
        String token = getSessionTokenFromCookies(cookies);
        if (token == null) {
            token = generateNewSessionToken();
        }
        this.sessionToken = token;
        this.attributeMap = SubmiteeServer.getInstance().createOrReadAttributeMap(this, Session.COLLECTION_NAME);

        this.lastUA = attributeMap.of("last-ua", String.class);
        this.lastActive = attributeMap.of("last-active", Date.class);
        this.lastActiveAddress = attributeMap.of("last-active-address", String.class);
        this.historyAddress = attributeMap.ofList("history-address", HistoryAddressEntry.class);
    }

    private static String generateNewSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String getSessionTokenFromCookies(Cookie[] cookies) {
        for (Cookie cookie : cookies) {
            if (Objects.equals(cookie.getName(), COOKIE_NAME_SESSION_TOKEN)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static SessionImpl createFromHttpRequest(HttpServletRequest request) {
        SessionImpl session = new SessionImpl(request.getSession(), request.getCookies());
        session.getAttributeMap().setAutoSaveAttribute(false);

        User user = SubmiteeServer.getInstance().resumeSession(session);
        if (user == null) {
            session.setUser(SubmiteeServer.getInstance().getAnonymousUserRealm().createAnonymousUser(session));
            session.setUser(new AnonymousUser(session.getSessionToken()));
        } else {
            session.setUser(user);
        }

        session.setLastUA(request.getHeader("User-Agent"));
        session.pushLastActive(request);

        session.getAttributeMap().setAutoSaveAttribute(true);
        request.getSession().setAttribute(HTTP_ATTRIBUTE_SESSION, session);
        return session;
    }

    private void pushLastActive(HttpServletRequest request) {
        String addr = Util.getRemoteAddr(request);
        Date now = new Date();

        lastActive.set(now);
        if (Objects.equals(addr, lastActiveAddress.get())) {
            lastActiveAddress.set(addr);
            historyAddress.add(new HistoryAddressEntry(now, addr));
        }
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String getLastUA() {
        return lastUA.get();
    }

    @Override
    public void setLastUA(String ua) {
        this.lastUA.set(ua);
    }

    @Override
    public Date getLastActive() {
        return lastActive.get();
    }

    @Override
    public List<HistoryAddressEntry> getHistoryAddresses() {
        return historyAddress.getList();
    }

    @Override
    public String getSessionToken() {
        return sessionToken;
    }

    public String getJSessionId() {
        return httpSession.getId();
    }

    @Override
    public String getAttributePersistKey() {
        return sessionToken;
    }

    @Override
    public AttributeMap<SessionImpl> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public void attributeUpdated(String path) {
        // TODO: 2021-03-25-0025
    }

    @Override
    public void close() {
        httpSession.setAttribute(HTTP_ATTRIBUTE_SESSION, null);
        SubmiteeServer.getInstance().removeAttributeMap(Session.COLLECTION_NAME, getAttributePersistKey());
        // TODO: 2021/3/23 invalidate token
    }
}
