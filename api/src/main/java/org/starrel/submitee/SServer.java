package org.starrel.submitee;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSerializer;
import org.starrel.submitee.auth.AuthScheme;
import org.starrel.submitee.blob.Blob;
import org.starrel.submitee.blob.BlobStorageProvider;
import org.starrel.submitee.model.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

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

    Blob createBlob(String blobStorageName, String fileName, String contentType, UserDescriptor uploader) throws IOException, SQLException;

    Blob getBlobByKey(String blobKey) throws Exception;

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> createTemporaryAttributeMap(TContext context);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> accessAttributeMap(TContext context, String collection);

    <TContext extends AttributeHolder<?>> AttributeMap<TContext> readAttributeMap(TContext context, String collection);

    boolean attributeMapExist(String persistId, String collection);

    <TValue> void addAttributeSerializer(Class<TValue> type, AttributeSerializer<TValue> serializer);

    User getUser(String realmType, String userId);

    User getUser(UserDescriptor userDescriptor);

    STemplate createTemplate() throws Exception;

    STemplate createTemplate(String grouping) throws Exception;

    STemplate getTemplate(UUID templateUUID) throws ExecutionException;

    List<? extends STemplate> getTemplateAllVersion(String templateId) throws ExecutionException;

    Set<String> getTemplateIds();

    STemplate getTemplateLatestVersion(String templateId) throws ExecutionException;

    Submission getSubmission(UUID uniqueId) throws ExecutionException;

    List<? extends Submission> getSubmissions(Bson query, Bson order) throws ExecutionException;

    List<UUID> getSubmissionIdsOfUser(UserDescriptor userDescriptor, Bson order);

    Submission createSubmission(UserDescriptor userDescriptor, STemplate template);

    List<? extends Session> getUserSession(UserDescriptor userDescriptor) throws ExecutionException;

    AuthScheme createPasswordAuthScheme();

    I18N.I18NKey getI18nKey(String key);

    void pushEvent(Level level, String entity, String activity, String detail);

    void pushEvent(Level level, String entity, String activity, Throwable stacktrace);

    void pushEvent(Level level, String entity, String activity, String detail, Throwable stacktrace);

    void pushEvent(Level level, Class<?> entity, String activity, String detail);

    void pushEvent(Level level, Class<?> entity, String activity, Throwable stacktrace);

    void pushEvent(Level level, Class<?> entity, String activity, String detail, Throwable stacktrace);

    Logger getLogger();

    List<UserRealm> getUserRealms();
}
