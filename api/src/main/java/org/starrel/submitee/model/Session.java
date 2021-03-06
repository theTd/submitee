package org.starrel.submitee.model;

import jakarta.servlet.http.HttpSession;
import org.starrel.submitee.attribute.AttributeHolder;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface Session extends AttributeHolder<Session> {
    String COLLECTION_NAME = "sessions";

    User getUser();

    void setUser(User user);

    String getLastUA();

    void setLastUA(String ua);

    Date getLastActive();

    String getLastActiveAddress();

    List<HistoryAddressEntry> getHistoryAddresses();

    String getSessionToken();

    void close(HttpSession httpSession);

    boolean isClosed();

    @Override
    default String getAttributePersistKey() {
        return getUser() == null ? null : getUser().getDescriptor().toString();
    }

    @Override
    default String getAttributeScheme() {
        return "Session";
    }
}
