package org.starrel.submitee.auth;

import org.starrel.submitee.model.User;

public interface AuthResult {

    boolean isAccepted();

    User getAcceptedUser();

    String getDenyClassify();

    String getDenyMessage();

    String getRedirect();
}
