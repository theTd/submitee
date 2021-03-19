package org.starrel.submitee;

import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.model.AuthScheme;
import org.starrel.submitee.model.NotificationScheme;
import org.starrel.submitee.model.UserRealm;

public interface SServer {

    void start() throws Exception;

    void shutdown() throws Exception;

    void addAuthScheme(AuthScheme authScheme);

    void addUserRealm(UserRealm userRealm);

    void addNotificationScheme(NotificationScheme notificationScheme);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context, String collection, String id);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection, String id);
}
