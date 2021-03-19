package org.starrel.submitee.model;

import org.starrel.submitee.attribute.AttributeHolder;

public interface Session extends AttributeHolder<Session> {

    User getUser();

    void setUser(User user);

    boolean isAnonymous();

    void close();
}
