package org.starrel.submitee.model;

import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeFilter;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;

import java.util.UUID;

public class STemplateImpl implements STemplate {
    private final UUID uniqueId;
    private final String templateId;
    private final int version;
    private final AttributeMap<STemplateImpl> attributeMap;

    private final AttributeSpec<UserDescriptor> committedBy;
    private final AttributeSpec<String> comment;

    public STemplateImpl(UUID uniqueId, String templateId, int version) {
        this.uniqueId = uniqueId;
        this.templateId = templateId;
        this.version = version;

        this.attributeMap = SubmiteeServer.getInstance().readAttributeMap(this, "templates");
        this.committedBy = attributeMap.of("committed-by", UserDescriptor.class);
        this.comment = attributeMap.of("comment", String.class);
    }

    public STemplateImpl(UUID uniqueId, String templateId, int version, AttributeMap<STemplateImpl> attributeMap) {
        this.uniqueId = uniqueId;
        this.templateId = templateId;
        this.version = version;
        this.attributeMap = attributeMap;
        this.committedBy = attributeMap.of("committed-by", UserDescriptor.class);
        this.comment = attributeMap.of("comment", String.class);
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getTemplateId() {
        return templateId;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getLatestVersion() {
        // TODO: 2021-03-25-0025 query latest version
        return 0;
    }

    @Override
    public User getCommittedBy() {
        return SubmiteeServer.getInstance().getUser(committedBy.get());
    }

    @Override
    public void setCommittedBy(User user) throws AttributeFilter.FilterException {
        this.committedBy.set(user.getDescriptor());
    }

    @Override
    public String getComment() {
        return comment.get();
    }

    @Override
    public void setComment(String comment) throws AttributeFilter.FilterException {
        this.comment.set(comment);
    }

    @Override
    public AttributeMap<? extends STemplate> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public void attributeUpdated(String path) {
        // TODO: 2021-03-25-0025
    }
}
