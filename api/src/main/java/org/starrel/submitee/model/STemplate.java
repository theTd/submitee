package org.starrel.submitee.model;

import org.starrel.submitee.attribute.AttributeHolder;

import java.util.UUID;

public interface STemplate extends AttributeHolder<STemplate> {

    UUID getUniqueId();

    String getTemplateId();

    int getVersion();

    int getLatestVersion();

    User getCommittedBy();

    void setCommittedBy(User user);

    String getComment();

    void setComment(String comment);

    @Override
    default String getAttributePersistKey() {
        return getUniqueId().toString();
    }
}
