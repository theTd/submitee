package org.starrel.submitee.model;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;

import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class AbstractUser implements User {
    private final AttributeMap<AbstractUser> attributeMap;
    private final UserDescriptor descriptor;
    private final AttributeSpec<String> preferredLanguage;

    protected AbstractUser(String realmType, String id) {
        this.descriptor = new UserDescriptor(realmType, id);
        this.attributeMap = SubmiteeServer.getInstance().accessAttributeMap(this, User.ATTRIBUTE_COLLECTION_NAME);
        this.preferredLanguage = this.attributeMap.of("preferred-language", String.class);
    }

    @Override
    public String getTypeId() {
        return descriptor.realmType;
    }

    @Override
    public String getId() {
        return descriptor.userId;
    }

    @Override
    public AttributeMap<? extends User> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public List<? extends Submission> getSubmissions(Bson query) throws ExecutionException {
        return SubmiteeServer.getInstance().getSubmissions(Filters.and(query,
                Filters.eq("body.submit-user", descriptor.toString())));
    }

    @Override
    public Submission createSubmission(STemplate template) {
        return SubmiteeServer.getInstance().createSubmission(descriptor, template);
    }

    @Override
    public String getPreferredLanguage() {
        return this.preferredLanguage.get();
    }

    @Override
    public void setPreferredLanguage(String language) {
        this.preferredLanguage.set(language);
    }

    @Override
    public UserDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean isSuperuser() {
        return false;
    }

    @Override
    public void setSuperuser(boolean superuser) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attributeUpdated(String path) {
    }

    @Override
    public String toString() {
        return descriptor.toString();
    }
}
