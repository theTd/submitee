package org.starrel.submitee.model;

import com.google.gson.JsonObject;
import org.starrel.submitee.attribute.AttributeHolder;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface Submission extends AttributeHolder<Submission> {
    String ATTRIBUTE_COLLECTION_NAME = "submissions";

    UUID getUniqueId();

    UserDescriptor getSubmitUserDescriptor();

    UUID getTemplateUUID();

    STemplate getTemplate() throws ExecutionException;

    Date getSubmitTime();

    JsonObject getBody();

    void setBody(JsonObject body);

    @Override
    default String getAttributePersistKey() {
        return getUniqueId().toString();
    }

    @Override
    default String getAttributeScheme() {
        return "Submission";
    }
}
