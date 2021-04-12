package org.starrel.submitee.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.I18N;
import org.starrel.submitee.ScriptRunner;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class InternalAccountRealm implements UserRealm {
    public final static String TYPE_ID = "internal";
    private final static Argon2 ARGON2;

    static {
        ARGON2 = Argon2Factory.create();
    }

    private final SubmiteeServer server;
    private final List<? extends AuthScheme> authSchemeList;
    private final Map<String, AuthScheme> authSchemeMap = new HashMap<>();

    private final Cache<Integer, InternalAccountUser> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    private final Cache<String, Integer> emailUidCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public InternalAccountRealm() throws SQLException, IOException {
        this.server = SubmiteeServer.getInstance();

        try (Connection conn = server.getDataSource().getConnection()) {
            ResultSet resultSet = conn.getMetaData().getTables(null, null, "internal_users", null);
            if (!resultSet.next()) {
                server.getLogger().info("creating table internal_users");

                URL resource = getClass().getResource("/internal_users.sql");
                new ScriptRunner(conn, true, true).runScript(new InputStreamReader(resource.openStream()));
            }
        }

        PasswordAuthScheme passwordAuthScheme = server.createPasswordAuthScheme();
        passwordAuthScheme.setHandler(new AuthHandler());
        authSchemeList = Collections.singletonList(passwordAuthScheme);
        authSchemeMap.put(passwordAuthScheme.getName(), passwordAuthScheme);

        server.getServletHandler().addServlet(InternalAccountServlet.class, "/internal-account/*");
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

    @Override
    public User resumeSession(Session session) {
        UserDescriptor loggedInUser = session.getAttribute("logged-in-user", UserDescriptor.class);
        if (loggedInUser != null && Objects.equals(loggedInUser.getRealmType(), TYPE_ID)) {
            try {
                return getUser(Integer.parseInt(loggedInUser.getUserId()));
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private InternalAccountUser getUser(int uid) throws ExecutionException {
        try {
            return cache.get(uid, () -> {
                try (Connection conn = server.getDataSource().getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM `internal_users` WHERE uid=?");
                    stmt.setInt(1, uid);
                    ResultSet r = stmt.executeQuery();
                    if (!r.next()) {
                        throw NotExistsSignal.INSTANCE;
                    } else {
                        return new InternalAccountUser(uid);
                    }
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NotExistsSignal) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public List<? extends AuthScheme> getSupportedAuthSchemes() {
        return authSchemeList;
    }

    @Override
    public AuthScheme getAuthScheme(String scheme) {
        return authSchemeMap.get(scheme);
    }

    public User createUser(String email, String password) throws SQLException, ExecutionException {
        email = email.toLowerCase(Locale.ROOT);
        password = hashPassword(password);
        try (Connection conn = server.getDataSource().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO internal_users(email,password) VALUES (?,?)");
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.executeUpdate();
            ResultSet r = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            r.next();
            int uid = r.getInt(1);
            return getUser(uid);
        }
    }

    public Integer getUidFromEmail(String email) throws ExecutionException {
        email = email.toLowerCase(Locale.ROOT);
        try {
            String finalEmail = email;
            return emailUidCache.get(finalEmail, () -> {
                try (Connection conn = server.getDataSource().getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("SELECT uid FROM internal_users WHERE email=?");
                    stmt.setString(1, finalEmail);
                    ResultSet r = stmt.executeQuery();
                    if (r.next()) {
                        return r.getInt(1);
                    }
                    throw NotExistsSignal.INSTANCE;
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NotExistsSignal) {
                return null;
            } else {
                throw e;
            }
        }
    }

    private class AuthHandler implements PasswordAuthScheme.AuthHandler {

        @Override
        public AuthResult handle(Session session, String username, String password) {
            try (Connection conn = server.getDataSource().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT password FROM internal_users WHERE username=?");
                stmt.setString(1, username);
                ResultSet r = stmt.executeQuery();
                if (!r.next()) return new AbstractAuthResult("user_not_exists",
                        I18N.General.USER_NOT_EXISTS.format(session), null);
                String storedPassword = r.getString(1);
                if (verifyPassword(password, storedPassword)) {
                    return new AbstractAuthResult(getUser(username), null);
                } else {
                    return new AbstractAuthResult("incorrect_password",
                            I18N.General.INCORRECT_PASSWORD.format(session), null);
                }
            } catch (Exception e) {
                ExceptionReporting.report(AuthHandler.class, "handle login request", e);
                return new AbstractAuthResult("internal_error",
                        I18N.General.INTERNAL_ERROR.format(session), null);
            }
        }
    }
}
