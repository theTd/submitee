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

    private final Cache<String, Integer> usernameUidCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public InternalAccountRealm() throws SQLException, IOException, ExecutionException {
        this.server = SubmiteeServer.getInstance();
        PasswordAuthScheme passwordAuthScheme = server.createPasswordAuthScheme();
        passwordAuthScheme.setHandler(new AuthHandler());
        authSchemeList = Collections.singletonList(passwordAuthScheme);
        authSchemeMap.put(passwordAuthScheme.getName(), passwordAuthScheme);
    }

    public void init() throws SQLException, IOException, ExecutionException {
        try (Connection conn = server.getDataSource().getConnection()) {
            ResultSet resultSet = conn.getMetaData().getTables(null, null, "internal_users", null);
            if (!resultSet.next()) {
                server.getLogger().info("creating table internal_users");

                URL resource = getClass().getResource("/internal_users.sql");
                new ScriptRunner(conn, true, true).runScript(new InputStreamReader(resource.openStream()));
            }
        }

        server.getServletHandler().addServlet(InternalAccountServlet.class, "/internal-account/*");

        if (System.getenv().containsKey("RESET-ADMIN-PASSWORD")) {
            String password = UUID.randomUUID().toString().substring(0, 8);
            InternalAccountUser admin = getUserFromUsernameOrEmail("admin");
            server.getLogger().warn("ADMIN PASSWORD is " + password);
            if (admin == null) {
                admin = createUserUsername("admin", password);
                server.getLogger().warn("created admin user");
            } else {
                admin.setPassword(password);
                server.getLogger().warn("reset admin password");
            }
            admin.setSuperuser(true);
        }
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
                return getUser(Integer.parseInt(loggedInUser.getUserId().substring(4)));
            } catch (ExecutionException e) {
                ExceptionReporting.report(InternalAccountRealm.class, "resuming session", e);
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

    public InternalAccountUser createUser(String email, String password) throws SQLException, ExecutionException {
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

    public InternalAccountUser createUserUsername(String username, String password) throws SQLException, ExecutionException {
        password = hashPassword(password);
        try (Connection conn = server.getDataSource().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO internal_users(username,password) VALUES (?,?)");
            stmt.setString(1, username);
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

    public Integer getUidFromUsername(String username) throws ExecutionException {
        username = username.toLowerCase(Locale.ROOT);
        try {
            String finalUsername = username;
            return usernameUidCache.get(finalUsername, () -> {
                try (Connection conn = server.getDataSource().getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("SELECT uid FROM internal_users WHERE username=?");
                    stmt.setString(1, finalUsername);
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

    public InternalAccountUser getUserFromUsernameOrEmail(String query) throws ExecutionException {
        // search for email or username
        Integer uid = getUidFromEmail(query);
        if (uid != null) return getUser(uid);
        uid = getUidFromUsername(query);
        if (uid != null) return getUser(uid);
        return null;
    }

    private class AuthHandler implements PasswordAuthScheme.AuthHandler {

        @Override
        public AuthResult handle(Session session, String username, String password) {
            Integer uid;
            try {
                uid = getUidFromUsername(username);
                if (uid == null) {
                    uid = getUidFromEmail(username);
                }
            } catch (ExecutionException e) {
                ExceptionReporting.report(AuthHandler.class, "searching user", e);
                return new AbstractAuthResult("internal_error",
                        I18N.General.INTERNAL_ERROR.format(session), null);
            }
            if (uid == null) {
                return new AbstractAuthResult("user_not_exists",
                        I18N.General.USER_NOT_EXIST.format(session), null);
            }

            try (Connection conn = server.getDataSource().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT password FROM internal_users WHERE uid=?");
                stmt.setInt(1, uid);
                ResultSet r = stmt.executeQuery();
                r.next();
                String storedPassword = r.getString(1);
                if (verifyPassword(password, storedPassword)) {
                    InternalAccountUser loggedIn = getUser(uid);
                    assert loggedIn != null;
                    session.setAttribute("logged-in-user", loggedIn.getDescriptor());
                    session.setAttribute("last-verify-password", System.currentTimeMillis());
                    session.getAttributeMap().save();
                    return new AbstractAuthResult(loggedIn, null);
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

        @Override
        public String getResetPasswordLink() {
            return "reset-password.html";
        }
    }
}
