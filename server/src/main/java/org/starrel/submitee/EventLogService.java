package org.starrel.submitee;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.zip.CRC32;

public class EventLogService {
    private final static long KEEP_IN_MEMORY_PERIOD = 3600 * 1000;
    private final static int WORK_THREADS = 2;

    private final Cache<Long, EventCollapseContext> crc32CollapseCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS).build();
    private final Cache<Long, Integer> crc32EidCache = CacheBuilder.newBuilder().build();

    private final ExecutorService executorService = Executors.newFixedThreadPool(WORK_THREADS);
    private DataSource dataSource;

    public void init() throws ExecutionException, InterruptedException, SQLException, IOException {
        this.dataSource = SubmiteeServer.getInstance().getDataSource();
        // init tables
        try (Connection conn = dataSource.getConnection()) {
            ResultSet r = conn.getMetaData().getTables(null, null, "event_occurs", null);
            if (!r.next()) {
                SubmiteeServer.getInstance().getLogger().info("creating table events,event_occurs");
                new ScriptRunner(conn, true, true)
                        .runScript(new InputStreamReader(getClass().getResourceAsStream("/events.sql")));
            }
        }

        List<EventCollapseContext> list = query(System.currentTimeMillis() - KEEP_IN_MEMORY_PERIOD,
                -1, null, null, null).get();
        for (EventCollapseContext context : list) {
            EventCollapseContext newContext = new EventCollapseContext(KEEP_IN_MEMORY_PERIOD,
                    context.crc32, context.levelText, context.entity, context.activity, context.detail);
            newContext.occurs.addAll(context.occurs);
            crc32CollapseCache.put(context.crc32, newContext);
        }
    }

    public void shutdown() throws InterruptedException {
        executorService.shutdown();
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            if (!executorService.isTerminated()) {
                SubmiteeServer.getInstance().getLogger().error(
                        String.format("%d tasks failed", executorService.shutdownNow().size()));
            }
        }
    }

    public void pushEvent(Level level, String entity, String activity, String detail) {
        if (level == Level.SEVERE) {
            SubmiteeServer.getInstance().getLogger().error(
                    String.format("sever event reported, entity=%s, activity=%s, detail=%s",
                            entity, activity, detail));
        }
        executorService.execute(new WriteTask(level, entity, activity, detail));
    }

    private List<Map.Entry<Integer, Long>> getOccursByTime(long start, int limit) throws SQLException {
        if (limit < 0) limit = 100;
        String sql = "SELECT `eid`,`time` FROM event_occurs";

        if (start != -1) {
            sql += " WHERE `time`<?";
        }

        sql += " ORDER BY `time` DESC LIMIT " + limit;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (start != -1) {
                stmt.setTimestamp(1, new Timestamp(start));
            }
            ResultSet r = stmt.executeQuery();
            List<Map.Entry<Integer, Long>> list = new LinkedList<>();
            while (r.next()) {
                int eid = r.getInt(1);
                long time = r.getTimestamp(2).getTime();
                list.add(new AbstractMap.SimpleEntry<>(eid, time));
            }
            return list;
        }
    }

    public CompletableFuture<List<EventCollapseContext>> query(long start, int limit, String levelText, String entity, String activity) {
        String level;
        if (levelText == null) {
            level = null;
        } else if (levelText.equalsIgnoreCase("info")) {
            level = "INFO";
        } else if (levelText.equalsIgnoreCase("warn")) {
            level = "WARN";
        } else if (levelText.equalsIgnoreCase("error")) {
            level = "ERROR";
        } else {
            level = null;
        }
        if (Objects.equals(entity, "")) {
            entity = null;
        }
        if (Objects.equals(activity, "")) {
            activity = null;
        }

        CompletableFuture<List<EventCollapseContext>> future = new CompletableFuture<>();
        String finalEntity = entity;
        String finalActivity = activity;
        executorService.submit(() -> {
            try (Connection conn = dataSource.getConnection()) {

                List<Map.Entry<Integer, Long>> occursByTime = getOccursByTime(start, limit);
                Map<Integer, EventCollapseContext> resultMap = new LinkedHashMap<>();
                for (Map.Entry<Integer, Long> entry : occursByTime) {
                    int eid = entry.getKey();
                    long time = entry.getValue();

                    EventCollapseContext context = resultMap.get(eid);
                    if (context != null) {
                        context.occurs.add(time);
                        continue;
                    }

                    String sql = "SELECT `crc32`,`level`,`entity`,`activity`,`detail` FROM `events` WHERE `id`=?";
                    if (level != null) {
                        sql += " AND `level`=?";
                    }
                    if (finalEntity != null) {
                        sql += " AND `entity`=?";
                    }
                    if (finalActivity != null) {
                        sql += " AND `activity`=?";
                    }
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    int idx = 1;
                    stmt.setInt(idx++, eid);
                    if (level != null) {
                        stmt.setString(idx++, level);
                    }
                    if (finalEntity != null) {
                        stmt.setString(idx++, finalEntity);
                    }
                    if (finalActivity != null) {
                        stmt.setString(idx, finalActivity);
                    }
                    ResultSet r = stmt.executeQuery();
                    if (r.next()) {
                        long crc32 = r.getLong(1);
                        String cLevel = r.getString(2);
                        String cEntity = r.getString(3);
                        String cActivity = r.getString(4);
                        String cDetail = r.getString(5);
                        context = new EventCollapseContext(-1, crc32, cLevel, cEntity, cActivity, cDetail);
                        context.occurs.add(time);
                    }

                    resultMap.put(eid, context);
                }

                resultMap.entrySet().removeIf(e -> e.getValue() == null);
                future.complete(new ArrayList<>(resultMap.values()));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private static String normalizeLevel(Level level) {
        if (level == null) level = Level.INFO;
        String levelText;
        if (level == Level.INFO) {
            levelText = "INFO";
        } else if (level == Level.WARNING) {
            levelText = "WARN";
        } else if (level == Level.SEVERE) {
            levelText = "ERROR";
        } else {
            levelText = "OTHER";
        }
        return levelText;
    }

    @AllArgsConstructor
    private class WriteTask implements Runnable {
        private final Level level;
        private final String entity;
        private final String activity;
        private final String detail;
        private final long time = System.currentTimeMillis();

        @SneakyThrows
        @Override
        public void run() {
            String levelText = normalizeLevel(level);
            CRC32 crc32 = new CRC32();
            crc32.update(levelText.getBytes(StandardCharsets.UTF_8));
            crc32.update(entity.getBytes(StandardCharsets.UTF_8));
            crc32.update(activity.getBytes(StandardCharsets.UTF_8));
            crc32.update(detail.getBytes(StandardCharsets.UTF_8));
            long crc32Value = crc32.getValue();

            EventCollapseContext collapse = crc32CollapseCache.get(crc32Value,
                    () -> new EventCollapseContext(KEEP_IN_MEMORY_PERIOD, crc32Value, levelText, entity, activity, detail));
            collapse.push();

            int retry = 0;
            while (retry++ < 10) {
                try {
                    int eid = crc32EidCache.get(crc32Value, () -> {
                        try (Connection conn = dataSource.getConnection()) {
                            PreparedStatement stmt = conn.prepareStatement("SELECT `id` FROM `events` WHERE `crc32`=?");
                            stmt.setLong(1, crc32Value);
                            ResultSet r = stmt.executeQuery();
                            if (!r.next()) {
                                stmt = conn.prepareStatement("INSERT INTO `events`(`level`,`entity`,`activity`,`detail`,`crc32`) VALUES (?,?,?,?,?)");
                                stmt.setString(1, levelText);
                                stmt.setObject(2, entity);
                                stmt.setObject(3, activity);
                                stmt.setObject(4, detail);
                                stmt.setLong(5, crc32Value);
                                stmt.executeUpdate();
                                r = stmt.executeQuery("SELECT LAST_INSERT_ID()");
                                r.next();
                            }
                            return r.getInt(1);
                        }
                    });
                    try (Connection conn = dataSource.getConnection()) {
                        PreparedStatement stmt = conn.prepareStatement("INSERT INTO `event_occurs`(`eid`,`time`) VALUES (?,?)");
                        stmt.setInt(1, eid);
                        stmt.setTimestamp(2, new Timestamp(time));
                        stmt.executeUpdate();
                    }
                    return;
                } catch (ExecutionException e) {
                    SubmiteeServer.getInstance().getLogger().warn("failed writing event: " + e.getMessage() + " retry: " + retry);
                    TimeUnit.SECONDS.sleep(1);
                }
            }
            SubmiteeServer.getInstance().getLogger().error("failed writing event retrying 10 times, aborted.");
        }
    }

    @AllArgsConstructor
    public static class EventCollapseContext {
        private final transient long period;
        private final LinkedList<Long> occurs = new LinkedList<>();
        private final long crc32;
        private final String levelText;
        private final String entity;
        private final String activity;
        private final String detail;

        synchronized void push() {
            occurs.add(System.currentTimeMillis());
            long invalidate = System.currentTimeMillis() - period;
            Iterator<Long> iterator = occurs.descendingIterator();
            while (iterator.hasNext()) {
                if (iterator.next() < invalidate) {
                    iterator.remove();
                }
            }
        }
    }
}
