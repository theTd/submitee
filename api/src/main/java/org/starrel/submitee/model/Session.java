package org.starrel.submitee.model;

import org.starrel.submitee.attribute.AttributeHolder;

import java.util.Date;

public interface Session extends AttributeHolder<Session> {
    String ATTRIBUTE_COLLECTION_NAME = "sessions";

    User getUser();

    void setUser(User user);

    boolean isAnonymous();

    String getLastUA();

    Date getLastActive();

    void close();

    @Override
    default String getAttributePersistKey() {
        return getUser() == null ? null : getUser().getDescriptor().toString();
    }

    @Override
    default String getAttributeScheme() {
        return "Session";
    }
}
