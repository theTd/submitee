package org.starrel.submitee.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;
import org.starrel.submitee.http.SFieldImpl;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class STemplateImpl implements STemplate, Comparable<STemplateImpl> {
    public final static Set<String> CONSTANT_ATTRIBUTES;

    static {
        CONSTANT_ATTRIBUTES = Set.copyOf(Arrays.stream(new String[]{
                "uuid", "grouping", "template-id", "version"
        }).collect(Collectors.toList()));
    }

    private final TemplateKeeper keeper;
    private final UUID uniqueId;
    private final String grouping;
    private final String templateId;
    private final int version;
    AtomicInteger latestVersion;
    private final AttributeMap<STemplateImpl> attributeMap;

    private final AttributeSpec<String> comment;
    private final AttributeSpec<JsonArray> fields;
    private final AttributeSpec<Boolean> published;
    private final AttributeSpec<UserDescriptor> publishedBy;
    private final AttributeSpec<Date> publishTime;
    private final AttributeSpec<Boolean> archived;

    STemplateImpl(TemplateKeeper keeper, UUID uniqueId, String grouping, String templateId, int version,
                  int latestVersion, boolean createAttributeMap, JsonObject content) {
        this.keeper = keeper;
        this.uniqueId = uniqueId;
        this.grouping = grouping;
        this.templateId = templateId;
        this.version = version;
        this.latestVersion = new AtomicInteger(latestVersion);

        this.attributeMap = SubmiteeServer.getInstance().accessAttributeMap(this, "templates");
        if (!createAttributeMap) this.attributeMap.read();
        if (content != null) this.attributeMap.setAll("", content);

        this.comment = attributeMap.of("comment", String.class);
        this.fields = attributeMap.of("fields", JsonArray.class);
        this.published = attributeMap.of("published", Boolean.class);
        this.publishedBy = attributeMap.of("published-by", UserDescriptor.class);
        this.publishTime = attributeMap.of("publish-time", Date.class);
        this.archived = attributeMap.of("archived", Boolean.class);

        if (createAttributeMap) {
            boolean save = this.attributeMap.getAutoSaveAttribute();
            this.attributeMap.setAutoSaveAttribute(false);
            this.attributeMap.set("uuid", uniqueId.toString());
            this.attributeMap.set("grouping", grouping);
            this.attributeMap.set("template-id", templateId);
            this.attributeMap.set("version", version);
            this.attributeMap.set("published", false);
            this.attributeMap.set("published-by", null);
            this.attributeMap.set("publish-time", null);
            this.attributeMap.set("archived", false);

            this.attributeMap.setAutoSaveAttribute(save);
        }
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
    public boolean isPublished() {
        Boolean published = this.published.get();
        return published != null && published;
    }

    @Override
    public void setPublished(boolean published) {
        this.published.set(published);
    }

    @Override
    public Date getPublishTime() {
        return this.publishTime.get();
    }

    @Override
    public void setPublishTime(Date time) {
        this.publishTime.set(time);
    }

    @Override
    public boolean isArchived() {
        return this.archived.get();
    }

    @Override
    public void setArchived(boolean archived) {
        this.archived.set(archived);
    }

    @Override
    public User getPublishedBy() {
        return SubmiteeServer.getInstance().getUser(publishedBy.get());
    }

    @Override
    public void setPublishedBy(UserDescriptor user) {
        this.publishedBy.set(user);
    }

    @Override
    public String getComment() {
        return comment.get();
    }

    @Override
    public void setComment(String comment) {
        this.comment.set(comment);
    }

    @Override
    public Map<String, SFieldImpl> getFields() {
        Map<String, SFieldImpl> fields = new LinkedHashMap<>();
        JsonArray jsonArray = this.fields.get();
        if (jsonArray != null) {
            for (JsonElement element : jsonArray) {
                SFieldImpl field = new SFieldImpl(this, element.getAsJsonObject());
                fields.put(field.getName(), field);
            }
        }
        return fields;
    }

    @Override
    public AttributeMap<? extends STemplate> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public void attributeUpdated(String path) {
        // TODO: 2021-03-25-0025
    }

    @Override
    public int compareTo(STemplateImpl o) {
        int id = getTemplateId().compareTo(o.getTemplateId());
        if (id != 0) return id;
        return getVersion() - o.getVersion();
    }

    @Override
    public boolean isPublicAccessible() {
        return isPublished();
    }

    @Override
    public String toString() {
        return uniqueId + "(" + templateId + ":" + version + ")";
    }
}
