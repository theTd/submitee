package org.starrel.submitee.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.ScriptRunner;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.User;
import org.starrel.submitee.model.UserRealm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class InternalAccountRealm implements UserRealm {
    public final static String TYPE_ID = "internal";
    private final static Argon2 ARGON2;

    private static InternalAccountUser ANONYMOUS;

    static {
        ARGON2 = Argon2Factory.create();
    }

    private final SubmiteeServer server;
    private final List<? extends AuthScheme> authSchemeList;
    private final Map<String, AuthScheme> authSchemeMap = new HashMap<>();

    private final Cache<Integer, InternalAccountUser> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public InternalAccountRealm(SubmiteeServer server) throws IOException, SQLException {
        this.server = server;
        ANONYMOUS = new InternalAccountUser(-1);

        PasswordAuthScheme passwordAuthScheme = server.createPasswordAuthScheme();
        passwordAuthScheme.setHandler(new AuthHandler());
        authSchemeList = Collections.singletonList(passwordAuthScheme);
        authSchemeMap.put(passwordAuthScheme.getName(), passwordAuthScheme);
    }

    public static boolean verifyPassword(String verify, String stored) {
        return ARGON2.verify(stored, verify.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashPassword(String password) {
        return ARGON2.hash(10, 65535, 1, password.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public InternalAccountUser getUser(String id) {
        int uid = Integer.parseInt(id);
        try {
            return getUser(uid);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private InternalAccountUser getUser(int uid) throws ExecutionException {
        return cache.get(uid, () -> {
            try (Connection conn = server.getDataSource().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM `internal_users` WHERE uid=?");
                stmt.setInt(1, uid);
                ResultSet r = stmt.executeQuery();
                return r.next() ? new InternalAccountUser(uid) : null;
            }
        });
    }

    @Override
    public User getAnonymousUser() {
        return ANONYMOUS;
    }

    @Override
    public List<? extends AuthScheme> getSupportedAuthSchemes() {
        return authSchemeList;
    }

    @Override
    public AuthScheme getAuthScheme(String scheme) {
        return authSchemeMap.get(scheme);
    }

    private class AuthHandler implements PasswordAuthScheme.AuthHandler {
        private final AuthResult RESULT_USER_NOT_EXISTS =
                new AbstractAuthResult("user.not_exists", null, null);
        private final AuthResult RESULT_INCORRECT_PASSWORD =
                new AbstractAuthResult("password.incorrect", null, null);
        private final AuthResult RESULT_INTERNAL_ERROR =
                new AbstractAuthResult("error.internal_error", null, null);

        private AuthHandler() throws SQLException, IOException {
            try (Connection conn = server.getDataSource().getConnection()) {
                ResultSet resultSet = conn.getMetaData().getTables(null, null, "internal_users", null);
                if (!resultSet.next()) {
                    server.getLogger().info("creating table internal_users");

                    URL resource = getClass().getResource("/internal_users.sql");
                    new ScriptRunner(conn, true, true).runScript(new InputStreamReader(resource.openStream()));
                }
            }
        }

        @Override
        public AuthResult handle(String username, String password) {
            try (Connection conn = server.getDataSource().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT password FROM internal_users WHERE username=?");
                stmt.setString(1, username);
                ResultSet r = stmt.executeQuery();
                if (!r.next()) return RESULT_USER_NOT_EXISTS;
                String storedPassword = r.getString(1);
                if (verifyPassword(password, storedPassword)) {
                    return new AbstractAuthResult(getUser(username), null);
                } else {
                    return RESULT_INCORRECT_PASSWORD;
                }
            } catch (Exception e) {
                ExceptionReporting.report(AuthHandler.class, "handle login request", e);
                return RESULT_INTERNAL_ERROR;
            }
        }
    }
}
