package org.starrel.submitee.auth;

import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeSpec;
import org.starrel.submitee.attribute.JdbcAttributeSource;
import org.starrel.submitee.model.AbstractUser;
import org.starrel.submitee.model.Session;

import java.util.concurrent.ExecutionException;

public class InternalAccountUser extends AbstractUser {
    private final int uid;

    private final AttributeSpec<String> username;
    private final AttributeSpec<String> password;
    private final AttributeSpec<String> email;
    private final AttributeSpec<String> sms;

    public InternalAccountUser(int uid) {
        super(InternalAccountRealm.TYPE_ID, "uid:" + uid);
        this.uid = uid;

        this.sms = getAttributeMap().of("sms", String.class);

        JdbcAttributeSource jdbcAttributeSource = new JdbcAttributeSource(SubmiteeServer.getInstance().getDataSource()
                , "internal_users", "uid=" + uid);
        (this.username = getAttributeMap().of("username", String.class)).setSource(jdbcAttributeSource);
        (this.password = getAttributeMap().of("password", String.class)).setSource(jdbcAttributeSource);
        (this.email = getAttributeMap().of("email", String.class)).setSource(jdbcAttributeSource);
    }

    public int getUid() {
        return uid;
    }

    public String getUsername() {
        return username.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public void setPassword(String password) throws ExecutionException {
        this.password.set(InternalAccountRealm.hashPassword(password));
        Session session = SubmiteeServer.getInstance().getUserSession(getDescriptor());
        if (session != null) {
            session.close();
        }
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
}
