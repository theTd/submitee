package org.starrel.submitee;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSerializer;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.blob.BlobStorageProvider;
import org.starrel.submitee.model.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface SServer {

    static SServer getInstance() {
        return APIBridge.instance;
    }

    void start() throws Exception;

    void shutdown() throws Exception;

    void addUserRealm(UserRealm userRealm);

    UserRealm getUserRealm(String name);

    void addNotificationScheme(NotificationScheme notificationScheme);

    void addBlobStorageProvider(BlobStorageProvider provider);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createAttributeMap(TContext context, String collection);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection);

    <TValue> void addAttributeSerializer(Class<TValue> type, AttributeSerializer<TValue> serializer);

    User getUser(String realmType, String userId);

    User getUser(UserDescriptor userDescriptor);

    STemplate createTemplate() throws Exception;

    STemplate createTemplate(String grouping) throws Exception;

    STemplate getTemplate(String templateId);

    STemplate getTemplateFromUUID(UUID templateUUID) throws ExecutionException;

    List<? extends STemplate> getTemplateAllVersion(String templateId);

    List<String> getTemplateIds();

    STemplate getTemplateLatestVersion(String templateId) throws ExecutionException;

    Submission getSubmission(UUID uniqueId);

    List<UUID> getSubmissionIdsOfUser(UserDescriptor userDescriptor);

    Submission createSubmission(UserDescriptor userDescriptor, STemplate template, JsonObject body);

    Session getUserSession(String userRealmTypeId, String userId);

    AuthScheme createPasswordAuthScheme();

    I18N.I18NKey getI18nKey(String key);

//    void reportException(Throwable throwable);
//
//    void reportException(String activity, Throwable throwable);

    void reportException(String entity, String activity, String detail);

    void reportException(String entity, String activity, Throwable stacktrace);

    Logger getLogger();
}
