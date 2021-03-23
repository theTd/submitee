package org.starrel.submitee.auth;

import javax.servlet.http.HttpServletRequest;

public interface AuthScheme {

    String getBootstrapPath();

    AuthResult auth(HttpServletRequest req);

}
