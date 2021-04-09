package org.starrel.submitee.model;

import org.starrel.submitee.attribute.AttributeFilter;
import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface STemplate extends AttributeHolder<STemplate> {

    UUID getUniqueId();

    String getTemplateId();

    int getVersion();

    int getLatestVersion() throws ExecutionException;

    User getCommittedBy();

    void setCommittedBy(User user) throws AttributeFilter.FilterException;

    String getComment();

    void setComment(String comment) throws AttributeFilter.FilterException;

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
