package org.starrel.submitee.auth;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.model.SubmissionImpl;
import org.starrel.submitee.model.User;
import org.starrel.submitee.model.UserDescriptor;

import java.util.List;

public class AnonymousUser implements User {
    private final UserDescriptor descriptor;
    private final String userId;
    private final AttributeMap<AnonymousUser> attributeMap;

    public AnonymousUser(String userId) {
        this.descriptor = new UserDescriptor(AnonymousUserRealm.TYPE_ID, userId);
        this.userId = userId;
        this.attributeMap = SubmiteeServer.getInstance().createOrReadAttributeMap(this, "users");
    }

    @Override
    public AttributeMap<? extends User> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public void attributeUpdated(String path) {
    }

    @Override
    public String getTypeId() {
        return AnonymousUserRealm.TYPE_ID;
    }

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public List<SubmissionImpl> getSubmissions(Bson query) {
        return SubmiteeServer.getInstance().getSubmissions(Filters.eq("user", getDescriptor().toString()));
    }

    @Override
    public UserDescriptor getDescriptor() {
        return descriptor;
    }
}
