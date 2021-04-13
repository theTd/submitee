package org.starrel.submitee.model;

import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeMapImpl;
import org.starrel.submitee.attribute.AttributeSpec;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SubmissionImpl implements Submission {
    private final UUID uniqueId;

    private final AttributeMap<SubmissionImpl> attributeMap;

    private final AttributeSpec<UUID> templateUniqueId;
    private final AttributeSpec<Date> submitTime;
    private final AttributeSpec<UserDescriptor> submitUser;

    SubmissionImpl(UserDescriptor user, STemplate template) {
        this(UUID.randomUUID());
        this.attributeMap.setAutoSaveAttribute(false);

        getAttributeMap().set("unique-id", uniqueId.toString());
        setTemplateUniqueId(template.getUniqueId());
        this.submitUser.set(user);
        this.submitTime.set(new Date());

        this.attributeMap.setAutoSaveAttribute(true);
    }

    SubmissionImpl(UUID uniqueId) {
        this.uniqueId = uniqueId;

        this.attributeMap = SubmiteeServer.getInstance().accessAttributeMap(this, Submission.ATTRIBUTE_COLLECTION_NAME);

        this.templateUniqueId = this.attributeMap.of("template-uuid", UUID.class);
        this.submitUser = this.attributeMap.of("submit-user", UserDescriptor.class);
        this.submitTime = this.attributeMap.of("submit-time", Date.class);
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public UserDescriptor getSubmitUserDescriptor() {
        return submitUser.get();
    }

    @Override
    public UUID getTemplateUniqueId() {
        return templateUniqueId.get();
    }

    @Override
    public void setTemplateUniqueId(UUID uuid) {
        this.templateUniqueId.set(uuid);
    }

    @Override
    public STemplateImpl getTemplate() throws ExecutionException {
        UUID id;
        if ((id = templateUniqueId.get()) == null) {
            return null;
        }
        return SubmiteeServer.getInstance().getTemplate(id);
    }

    @Override
    public Date getSubmitTime() {
        return submitTime.get();
    }

    @Override
    public JsonObject getBody() {
        return attributeMap.get("body", JsonObject.class);
    }

    @Override
    public void setBody(JsonObject body) {
        attributeMap.set("body", body);
    }

    @Override
    public AttributeMap<SubmissionImpl> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public void attributeUpdated(String path) {
    }
}
