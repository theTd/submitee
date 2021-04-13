package org.starrel.submitee.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.ScriptRunner;
import org.starrel.submitee.SubmiteeServer;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
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
            cacheGrouping();
            cacheLatestVersions();
        } catch (Exception t) {
            throw new Exception("failed initializing template keeper", t);
        }
    }

    private void initTable() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet r = conn.getMetaData().getTables(null, null, "templates", null);
            if (!r.next()) {
                SubmiteeServer.getInstance().getLogger().info("creating table templates");
                new ScriptRunner(conn, true, true).runScript(new InputStreamReader(getClass().getResourceAsStream("/templates.sql")));

            }
        }
    }

    private void cacheGrouping() throws SQLException, ExecutionException {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet r = conn.createStatement().executeQuery("SELECT DISTINCT `grouping` FROM templates");
            while (r.next()) {
                getGroupingId(r.getString(1));
            }
        }
    }

    private void cacheLatestVersions() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet r = conn.createStatement()
                    .executeQuery("SELECT DISTINCT `template_id`, MAX(version) FROM templates GROUP BY template_id");
            while (r.next()) {
                String templateId = r.getString(1);
                int version = r.getInt(2);
                ResultSet _r = conn.createStatement().executeQuery(
                        "SELECT uuid FROM templates WHERE template_id=\"" + templateId + "\" AND version=" + version);
                _r.next();
                latestVersionCache.put(templateId, UUID.fromString(_r.getString(1)));
            }
        }
    }

    public STemplateImpl createNewTemplate(String grouping) throws Exception {
        if (grouping == null || grouping.isEmpty()) throw new IllegalArgumentException("grouping");

        grouping = grouping.toUpperCase();
        UUID uniqueId = UUID.randomUUID();
        try (Connection conn = dataSource.getConnection()) {
            String templateId = allocateTemplateId(grouping);

            PreparedStatement stmt = conn.prepareStatement("INSERT INTO `templates`(`uuid`,`grouping`,`template_id`,`version`) VALUES (?,?,?,0)");
            stmt.setString(1, uniqueId.toString());
            stmt.setString(2, grouping);
            stmt.setString(3, templateId);
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

    private AtomicInteger getGroupingId(String grouping) throws ExecutionException {
        return groupingCache.get(grouping, () -> {
            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT count(*) OVER () FROM `templates` WHERE `grouping`=? GROUP BY `template_id` LIMIT 1");
                stmt.setString(1, grouping);
                ResultSet r = stmt.executeQuery();
                if (r.next()) {
                    return new AtomicInteger(r.getInt(1));
                } else {
                    return new AtomicInteger(0);
                }
            }
        });
    }

    private String allocateTemplateId(String grouping) throws ExecutionException {
        AtomicInteger id = getGroupingId(grouping);
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
                    if (!r.next()) throw NotExistsSignal.INSTANCE;
                    return UUID.fromString(r.getString(1));
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NotExistsSignal) {
                return null;
            } else {
                throw e;
            }
        }
        return getTemplate(latestVersion);
    }

    public Set<String> getIds() {
        return latestVersionCache.asMap().keySet();
    }

    public List<UUID> getUniqueIdsByQuery(Bson filters) {
        MongoCollection<Document> collection = SubmiteeServer.getInstance().getMongoDatabase().getCollection("templates");
        MongoCursor<Document> cursor = collection.find(filters).projection(Projections.fields(Projections.include("id"))).cursor();

        List<UUID> list = new LinkedList<>();
        while (cursor.hasNext()) {
            list.add(UUID.fromString(cursor.next().getString("id")));
        }
        return list;
    }

    public List<STemplateImpl> getByQuery(Bson filters) throws ExecutionException {
        List<STemplateImpl> list = new ArrayList<>();
        for (UUID uuid : getUniqueIdsByQuery(filters)) {
            STemplateImpl template = getTemplate(uuid);
            if (template == null) {
                ExceptionReporting.report(TemplateKeeper.class, "template mismatch", "could not find template with uuid=" + uuid);
            } else {
                list.add(template);
            }
        }
        return list;
    }

    public STemplateImpl getTemplate(UUID uniqueId) throws ExecutionException {
        try {
            return templateCache.get(uniqueId, () -> {
                try (Connection conn = dataSource.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("SELECT `grouping`,`template_id`,`version` FROM `templates` WHERE uuid=?");
                    stmt.setString(1, uniqueId.toString());
                    ResultSet r = stmt.executeQuery();
                    if (!r.next()) {
                        throw NotExistsSignal.INSTANCE;
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
            if (e.getCause() instanceof NotExistsSignal) {
                return null;
            } else {
                throw e;
            }
        }
    }
}
