package org.starrel.submitee.auth;

import org.starrel.submitee.model.User;

public class AbstractAuthResult implements AuthResult {
    private final User acceptedUser;
    private final String denyClassify;
    private final String denyMessage;
    private final String redirect;

    public AbstractAuthResult(User acceptedUser, String redirect) {
        this.acceptedUser = acceptedUser;
        this.denyClassify = null;
        this.denyMessage = null;
        this.redirect = redirect;
    }

    public AbstractAuthResult(String denyClassify, String denyMessage, String redirect) {
        this.acceptedUser = null;
        this.denyClassify = denyClassify;
        this.denyMessage = denyMessage;
        this.redirect = redirect;
    }

    @Override
    public boolean isAccepted() {
        return acceptedUser != null;
    }

    @Override
    public User getAcceptedUser() {
        return acceptedUser;
    }

    @Override
    public String getDenyClassify() {
        return denyClassify;
    }

    @Override
    public String getDenyMessage() {
        return denyMessage;
    }

    @Override
    public String getRedirect() {
        return null;
    }
}
