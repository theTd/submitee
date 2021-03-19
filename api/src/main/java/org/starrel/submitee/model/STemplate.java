package org.starrel.submitee.model;

import org.starrel.submitee.attribute.AttributeHolder;

import java.util.UUID;

public interface STemplate extends AttributeHolder<STemplate> {

    UUID getUniqueId();

    int getVersion();

    int getLatestVersion();

    User getCommittedBy();

    String getComment();

    @Override
    default String getAttributePersistKey() {
        return getUniqueId().toString();
    }
}
