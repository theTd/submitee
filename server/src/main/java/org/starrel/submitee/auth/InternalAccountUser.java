package org.starrel.submitee.auth;

import org.bson.conversions.Bson;
import org.starrel.submitee.SServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;
import org.starrel.submitee.model.Submission;
import org.starrel.submitee.model.User;

import java.util.List;

public class InternalAccountUser implements User {
    private final int uid;
    private final String username;
    private final String password;
    private final AttributeMap<InternalAccountUser> attributeMap;

    private final AttributeSpec<String> email;
    private final AttributeSpec<String> sms;

    public InternalAccountUser(int uid, String username, String password) {
        this.uid = uid;
        this.username = username;
        this.password = password;
        this.attributeMap = SServer.getInstance().readAttributeMap(this,
                User.ATTRIBUTE_COLLECTION_NAME, getAttributePersistKey());

        this.email = attributeMap.of("email", String.class);
        this.sms = attributeMap.of("sms", String.class);
    }

    @Override
    public boolean isAnonymous() {
        return uid > 0;
    }

    @Override
    public String getTypeId() {
        return InternalAccountRealm.TYPE_ID;
    }

    @Override
    public String getId() {
        return uid > 0 ? "uid:" + uid : null;
    }

    @Override
    public List<Submission> getSubmissions(Bson query) {
        return null;
    }

    public String getEmail() {
        return getAttribute("email", String.class);
    }

    public void setEmail(String email) {
        setAttribute("email", email);
    }

    public String getSMS() {
        return getAttribute("sms", String.class);
    }

    public void setSMS(String sms) {
        setAttribute("sms", sms);
    }

    @Override
    public String getAttributePersistKey() {
        return getId();
    }

    @Override
    public AttributeMap<User> getAttributeMap() {
        return null;
    }

    @Override
    public void attributeUpdated(String path) {

    }
}
