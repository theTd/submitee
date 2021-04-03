package org.starrel.submitee.model;

import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeFilter;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class STemplateImpl implements STemplate {
    private final TemplateKeeper keeper;
    private final UUID uniqueId;
    private final String grouping;
    private final String templateId;
    private final int version;
    AtomicInteger latestVersion;
    private final AttributeMap<STemplateImpl> attributeMap;

    private final AttributeSpec<UserDescriptor> committedBy;
    private final AttributeSpec<String> comment;

    public STemplateImpl(TemplateKeeper keeper, UUID uniqueId, String grouping, String templateId, int version, int latestVersion, boolean createAttributeMap) {
        this.keeper = keeper;
        this.uniqueId = uniqueId;
        this.grouping = grouping;
        this.templateId = templateId;
        this.version = version;
        this.latestVersion = new AtomicInteger(latestVersion);

        this.attributeMap = createAttributeMap ? SubmiteeServer.getInstance().readAttributeMap(this, "templates") :
                SubmiteeServer.getInstance().readAttributeMap(this, "templates");
        this.committedBy = attributeMap.of("committed-by", UserDescriptor.class);
        this.comment = attributeMap.of("comment", String.class);
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getGrouping() {
        return grouping;
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
    public int getLatestVersion() throws ExecutionException {
        STemplateImpl latest = keeper.getTemplateLatestVersion(templateId);
        return latest == this ? latestVersion.get() : latest.latestVersion.get();
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
