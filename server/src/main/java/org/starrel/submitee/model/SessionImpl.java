package org.starrel.submitee.model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.Util;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class SessionImpl implements Session {
    private final SessionKeeper keeper;
    private final String sessionToken;

    private final AttributeMap<SessionImpl> attributeMap;

    private final AttributeSpec<String> lastUA;
    private final AttributeSpec<Date> lastActive;
    private final AttributeSpec<String> lastActiveAddress;
    private final AttributeSpec<HistoryAddressEntry> historyAddress;

    private HttpSession httpSession;
    private User user;
    private boolean closed = false;

    SessionImpl(SessionKeeper keeper, String sessionToken) {
        this.keeper = keeper;
        this.sessionToken = sessionToken;
        this.attributeMap = SubmiteeServer.getInstance().accessAttributeMap(this, Session.COLLECTION_NAME);
        this.attributeMap.setAutoSaveAttribute(false);

        this.lastUA = attributeMap.of("last-ua", String.class);
        this.lastActive = attributeMap.of("last-active", Date.class);
        this.lastActiveAddress = attributeMap.of("last-active-address", String.class);

        this.historyAddress = attributeMap.ofList("history-address", HistoryAddressEntry.class);
    }

    void setHttpSession(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    void pushLastActive(HttpServletRequest request) {
        String addr = Util.getRemoteAddr(request);
        Date now = new Date();

        lastActive.set(now);
        if (!Objects.equals(addr, lastActiveAddress.get())) {
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
    public String getLastActiveAddress() {
        return lastActiveAddress.get();
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
        if (closed) {
            throw new RuntimeException("updating attribute map on a closed session");
        }
    }

    @Override
    public void close() {
        if (httpSession != null) {
            httpSession.removeAttribute(SessionKeeper.HTTP_ATTRIBUTE_SESSION);
        }
        this.attributeMap.delete();
        SubmiteeServer.getInstance().removeAttributeMap(Session.COLLECTION_NAME, getAttributePersistKey());
        keeper.remove(this);
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
