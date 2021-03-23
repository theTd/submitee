package org.starrel.submitee;

import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSerializer;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.blob.BlobStorage;
import org.starrel.submitee.model.NotificationScheme;
import org.starrel.submitee.model.Session;
import org.starrel.submitee.model.UserRealm;

import java.util.logging.Logger;

public interface SServer {

    static SServer getInstance() {
        return APIBridge.instance;
    }

    void start() throws Exception;

    void shutdown() throws Exception;

    void addAuthScheme(AuthScheme authScheme);

    void addUserRealm(UserRealm userRealm);

    void addNotificationScheme(NotificationScheme notificationScheme);

    void addBlobStorage(BlobStorage blobStorage);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context, String collection, String id);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection, String id);

    <TValue> void addAttributeSerializer(Class<TValue> type, AttributeSerializer<TValue> serializer);

    Session getUserSession(String userRealmTypeId, String userId);

    AuthScheme createPasswordAuthScheme();

    I18N.I18NKey getI18nKey(String key);

    void reportException(Throwable throwable);

    void reportException(String activity, Throwable throwable);

    Logger getLogger();
}
