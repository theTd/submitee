package org.starrel.submitee;

import org.slf4j.Logger;
import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSerializer;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.blob.BlobStorage;
import org.starrel.submitee.model.*;

import java.util.List;

public interface SServer {

    static SServer getInstance() {
        return APIBridge.instance;
    }

    void start() throws Exception;

    void shutdown() throws Exception;

    void addAuthScheme(AuthScheme authScheme);

    void addUserRealm(UserRealm userRealm);

    UserRealm getUserRealm(String name);

    void addNotificationScheme(NotificationScheme notificationScheme);

    void addBlobStorage(BlobStorage blobStorage);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context, String collection, String id);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection, String id);

    <TValue> void addAttributeSerializer(Class<TValue> type, AttributeSerializer<TValue> serializer);

    User getUser(String realmType, String userId);

    User getUser(UserDescriptor userDescriptor);

    STemplate createTemplate(String templateId);

    STemplate getTemplate(String templateId);

    List<? extends STemplate> getTemplateAllVersion(String templateId);

    List<String> getTemplateIds();

    int getTemplateLatestVersion(String templateId);

    Session getUserSession(String userRealmTypeId, String userId);

    AuthScheme createPasswordAuthScheme();

    I18N.I18NKey getI18nKey(String key);

    void reportException(Throwable throwable);

    void reportException(String activity, Throwable throwable);

    Logger getLogger();
}
