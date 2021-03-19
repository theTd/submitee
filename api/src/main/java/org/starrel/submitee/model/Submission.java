package org.starrel.submitee.model;

import org.starrel.submitee.attribute.AttributeHolder;

import java.util.Date;
import java.util.UUID;

public interface Submission extends AttributeHolder<Submission> {
    UUID getUniqueId();

    User getUser();

    STemplate getTemplate();

    Date submitTime();

    @Override
    default String getAttributePersistKey() {
        return getUniqueId().toString();
    }
}
