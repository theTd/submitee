package org.starrel.submitee.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.starrel.submitee.SubmiteeServer;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TemplateKeeper {
    public final static String DEFAULT_GROUPING = "DEF";
    private final Cache<UUID, STemplateImpl> templateCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS).maximumSize(1000).build();

    private final Cache<String, UUID> latestVersionCache = CacheBuilder.newBuilder().maximumSize(1000).build();
    private final Cache<String, AtomicInteger> groupingCache = CacheBuilder.newBuilder().build();

    private final DataSource dataSource;

    public TemplateKeeper(SubmiteeServer submiteeServer) {
        this.dataSource = submiteeServer.getDataSource();
    }

    public void init() throws Exception {
        try {
            initTable();
        } catch (SQLException t) {
            throw new Exception("failed initializing template keeper", t);
        }
    }

    private void initTable() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet r = conn.getMetaData().getTables(null, null, "templates", null);
            if (!r.next()) {
                SubmiteeServer.getInstance().getLogger().info("creating table templates");
                new ScriptRunner(conn).runScript(new InputStreamReader(getClass().getResourceAsStream("/templates.sql")));
            }
        }
    }

    public STemplateImpl createNewTemplate(String grouping) throws Exception {
        if (grouping == null || grouping.isEmpty()) throw new IllegalArgumentException("grouping");

        grouping = grouping.toUpperCase();
        UUID uniqueId = UUID.randomUUID();
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO `templates`(`uuid`,`grouping`) VALUES (?,?)");
            stmt.setString(1, uniqueId.toString());
            stmt.setString(2, grouping);
            stmt.executeUpdate();
            ResultSet r = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            r.next();
            int insertedId = r.getInt(1);
            String templateId = allocateTemplateId(grouping);
            stmt = conn.prepareStatement("UPDATE `templates` SET `template_id`=? WHERE `id`=?");
            stmt.setString(1, templateId);
            stmt.setInt(2, insertedId);
            stmt.executeUpdate();

            STemplateImpl t = new STemplateImpl(this, uniqueId, grouping, templateId, 0, 0, true);
            latestVersionCache.put(templateId, uniqueId);
            templateCache.put(uniqueId, t);
            return t;
        }
    }

    public STemplateImpl createRevisionTemplate(String templateId) throws Exception {
        STemplateImpl revision = getTemplateLatestVersion(templateId);
        if (revision == null) throw new RuntimeException("failed to load revision template");

        int version = revision.latestVersion.incrementAndGet();
        UUID uniqueId = UUID.randomUUID();
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO `templates`(`uuid`,`grouping`,`template_id`,`version`) VALUES (?,?,?,?)");
            stmt.setString(1, uniqueId.toString());
            stmt.setString(2, revision.getGrouping());
            stmt.setString(3, revision.getTemplateId());
            stmt.setInt(4, version);
            stmt.executeUpdate();

            STemplateImpl t = new STemplateImpl(this, uniqueId, revision.getGrouping(), templateId, version, version, true);
            latestVersionCache.put(templateId, uniqueId);
            templateCache.put(uniqueId, t);
            return t;
        }
    }

    @SneakyThrows
    private String allocateTemplateId(String grouping) {
        AtomicInteger id = groupingCache.get(grouping, () -> {
            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT count(*) FROM `templates` WHERE `grouping`=? GROUP BY `template_id`");
                stmt.setString(1, grouping);
                ResultSet r = stmt.executeQuery();
                r.next();
                return new AtomicInteger(r.getInt(1));
            }
        });
        return grouping + "-" + id.incrementAndGet();
    }

    public STemplateImpl getTemplateLatestVersion(String templateId) throws ExecutionException {
        UUID latestVersion;
        try {
            latestVersion = latestVersionCache.get(templateId, () -> {
                try (Connection conn = dataSource.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("SELECT `uuid` FROM `templates` WHERE `template_id`=? ORDER BY `version` DESC LIMIT 1");
                    stmt.setString(1, templateId);
                    ResultSet r = stmt.executeQuery();
                    if (!r.next()) throw TemplateNotExistsSignal.INSTANCE;
                    return UUID.fromString(r.getString(1));
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TemplateNotExistsSignal) {
                return null;
            } else {
                throw e;
            }
        }
        return getTemplate(latestVersion);
    }

    public List<STemplateImpl> getTemplateAllVersions(String templateId) {
        // TODO: 2021/4/4
        return null;
    }

    public STemplateImpl getTemplate(UUID uniqueId) throws ExecutionException {
        try {
            return templateCache.get(uniqueId, () -> {
                try (Connection conn = dataSource.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("SELECT `grouping`,`template_id`,`version` FROM `templates` WHERE uuid=?");
                    stmt.setString(1, uniqueId.toString());
                    ResultSet r = stmt.executeQuery();
                    if (!r.next()) {
                        throw TemplateNotExistsSignal.INSTANCE;
                    }
                    String grouping = r.getString(1);
                    String templateId = r.getString(2);
                    int version = r.getInt(3);
                    UUID latest = latestVersionCache.getIfPresent(templateId);

                    int latestVersion;
                    if (!Objects.equals(latest, uniqueId)) {
                        stmt = conn.prepareStatement("SELECT `uuid`,`version` FROM `templates` WHERE `template_id`=? ORDER BY `version` DESC LIMIT 1");
                        stmt.setString(1, templateId);
                        r = stmt.executeQuery();
                        r.next();
                        latest = UUID.fromString(r.getString(1));
                        latestVersion = r.getInt(2);
                        latestVersionCache.put(templateId, latest);
                    } else {
                        latestVersion = version;
                    }
                    return new STemplateImpl(this, uniqueId, grouping, templateId, version, latestVersion, false);
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TemplateNotExistsSignal) {
                return null;
            } else {
                throw e;
            }
        }
    }
}
