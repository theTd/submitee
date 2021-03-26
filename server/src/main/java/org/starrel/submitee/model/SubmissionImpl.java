package org.starrel.submitee.model;

import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;

import java.util.Date;
import java.util.UUID;

public class SubmissionImpl implements Submission {
    private final UUID uniqueId;

    private final AttributeMap<SubmissionImpl> attributeMap;

    private final AttributeSpec<UUID> templateUUIDSpec;
    private final AttributeSpec<Date> submitTimeSpec;
    private final AttributeSpec<UserDescriptor> submitUserDescriptorSpec;

    public SubmissionImpl(UserDescriptor submitUserDescriptor, STemplateImpl template) {
        this.uniqueId = UUID.randomUUID();
        this.attributeMap = SubmiteeServer.getInstance().createAttributeMap(this, Submission.ATTRIBUTE_COLLECTION_NAME);

        this.templateUUIDSpec = this.attributeMap.of("template-id", UUID.class);
        this.submitUserDescriptorSpec = this.attributeMap.of("submit-user", UserDescriptor.class);
        this.submitTimeSpec = this.attributeMap.of("submit-time", Date.class);

        this.templateUUIDSpec.set(template.getUniqueId());
        this.submitUserDescriptorSpec.set(submitUserDescriptor);
        this.submitTimeSpec.set(new Date());
    }

    public SubmissionImpl(UUID uniqueId) {
        this.uniqueId = uniqueId;

        this.attributeMap = SubmiteeServer.getInstance().readAttributeMap(this, ATTRIBUTE_COLLECTION_NAME);
        this.templateUUIDSpec = this.attributeMap.of("template-id", UUID.class);
        this.submitUserDescriptorSpec = this.attributeMap.of("submit-user", UserDescriptor.class);
        this.submitTimeSpec = this.attributeMap.of("submit-time", Date.class);
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public UserDescriptor getSubmitUserDescriptor() {
        return submitUserDescriptorSpec.get();
    }

    @Override
    public UUID getTemplateUUID() {
        return templateUUIDSpec.get();
    }

    @Override
    public STemplateImpl getTemplate() {
        UUID id;
        if ((id = templateUUIDSpec.get()) == null) {
            return null;
        }
        return SubmiteeServer.getInstance().getTemplateFromUUID(id);
    }

    @Override
    public Date getSubmitTime() {
        return submitTimeSpec.get();
    }

    @Override
    public JsonObject getBody() {
        JsonObject att = attributeMap.serialize();
        if (att.has("body")) {
            return att.get("body").getAsJsonObject();
        }
        return null;
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
