package org.starrel.submitee.auth;

import org.bson.conversions.Bson;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.model.Submission;
import org.starrel.submitee.model.User;

import java.util.List;

public class InternalAccountUser implements User {
    private final String id;

    public InternalAccountUser(String id) {
        this.id = id;
    }

    @Override
    public boolean isAnonymous() {
        return id == null;
    }

    @Override
    public String getTypeId() {
        return InternalAccountRealm.TYPE_ID;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Submission> getSubmissions(Bson query) {
        return null;
    }

    @Override
    public String getAttributePersistKey() {
        return id;
    }

    @Override
    public AttributeMap<User> getAttributeMap() {
        return null;
    }

    @Override
    public void attributeUpdated(String path) {

    }
}
