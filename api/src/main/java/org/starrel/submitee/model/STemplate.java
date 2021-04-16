package org.starrel.submitee.model;

import org.starrel.submitee.attribute.AttributeHolder;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface STemplate extends AttributeHolder<STemplate> {

    UUID getUniqueId();

    String getTemplateId();

    int getVersion();

    int getLatestVersion() throws ExecutionException;

    User getPublishedBy();

    void setPublishedBy(UserDescriptor user);

    String getComment();

    void setComment(String comment);

    boolean isPublished();

    void setPublished(boolean published);

    Date getPublishTime();

    void setPublishTime(Date time);

    boolean isArchived();

    void setArchived(boolean archived);

    Map<String, ? extends SField> getFields();

    @Override
    default String getAttributePersistKey() {
        return getUniqueId().toString();
    }

    @Override
    default String getAttributeScheme() {
        return "STemplate";
    }
}
