package org.starrel.submitee.auth;

import org.bson.conversions.Bson;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;
import org.starrel.submitee.attribute.JdbcAttributeSource;
import org.starrel.submitee.model.Submission;
import org.starrel.submitee.model.User;
import org.starrel.submitee.model.UserDescriptor;

import java.util.List;

public class InternalAccountUser implements User {
    private final int uid;
    private final UserDescriptor descriptor;
    private final AttributeMap<InternalAccountUser> attributeMap;

    private final AttributeSpec<String> username;
    private final AttributeSpec<String> password;
    private final AttributeSpec<String> email;
    private final AttributeSpec<String> sms;

    public InternalAccountUser(int uid) {
        this.uid = uid;
        this.descriptor = UserDescriptor.builder().realmType(getTypeId()).userId(getAttributePersistKey()).build();

        this.attributeMap = SubmiteeServer.getInstance().readAttributeMap(this, User.ATTRIBUTE_COLLECTION_NAME);

        this.email = attributeMap.of("email", String.class);
        this.sms = attributeMap.of("sms", String.class);

        JdbcAttributeSource jdbcAttributeSource = new JdbcAttributeSource(SubmiteeServer.getInstance().getDataSource()
                , "internal_users", "id=" + uid);
        (this.username = attributeMap.of("username", String.class)).setSource(jdbcAttributeSource);
        (this.password = attributeMap.of("password", String.class)).setSource(jdbcAttributeSource);
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

    @Override
    public UserDescriptor getDescriptor() {
        return descriptor;
    }

    public String getUsername() {
        return username.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public String getPassword() {
        return password.get();
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public String getSMS() {
        return sms.get();
    }

    public void setSMS(String sms) {
        this.sms.set(sms);
    }

    @Override
    public String getAttributePersistKey() {
        return isAnonymous() ? null : getId();
    }

    @Override
    public AttributeMap<InternalAccountUser> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public void attributeUpdated(String path) {
        // TODO: 2021-03-25-0025
    }
}
