package org.starrel.submitee;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSerializer;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.blob.BlobStorage;
import org.starrel.submitee.model.*;

import java.util.List;
import java.util.UUID;

public interface SServer {

    static SServer getInstance() {
        return APIBridge.instance;
    }

    void start() throws Exception;

    void shutdown() throws Exception;

    void addUserRealm(UserRealm userRealm);

    UserRealm getUserRealm(String name);

    void addNotificationScheme(NotificationScheme notificationScheme);

    void addBlobStorage(BlobStorage blobStorage);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context, String collection);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection);

    <TValue> void addAttributeSerializer(Class<TValue> type, AttributeSerializer<TValue> serializer);

    User getUser(String realmType, String userId);

    User getUser(UserDescriptor userDescriptor);

    STemplate createTemplate(String templateId);

    STemplate getTemplate(String templateId);

    STemplate getTemplateFromUUID(UUID templateUUID);

    List<? extends STemplate> getTemplateAllVersion(String templateId);

    List<String> getTemplateIds();

    int getTemplateLatestVersion(String templateId);

    Submission getSubmission(UUID uniqueId);

    List<UUID> getSubmissionIdsOfUser(UserDescriptor userDescriptor);

    Submission createSubmission(UserDescriptor userDescriptor, STemplate template, JsonObject body);

    Session getUserSession(String userRealmTypeId, String userId);

    AuthScheme createPasswordAuthScheme();

    I18N.I18NKey getI18nKey(String key);

    void reportException(Throwable throwable);

    void reportException(String activity, Throwable throwable);

    void reportException(String event);

    Logger getLogger();
}
