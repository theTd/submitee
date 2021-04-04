package org.starrel.submitee.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.starrel.submitee.ScriptRunner;
import org.starrel.submitee.SubmiteeServer;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ObjectMapController {
    private final DataSource dataSource;

    private final Cache<UUID, String> cache = CacheBuilder.newBuilder().maximumSize(1000).build();

    public ObjectMapController(SubmiteeServer server) {
        this.dataSource = server.getDataSource();
    }

    public void init() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet r = conn.getMetaData().getTables(null, null, "object_map", null);
            if (!r.next()) {
                new ScriptRunner(conn, true, true).runScript(new InputStreamReader(getClass().getResourceAsStream("/object_map.sql")));
            }
        }
    }

    public String getType(UUID uuid) throws ExecutionException {
        try {
            return cache.get(uuid, () -> {
                try (Connection conn = dataSource.getConnection()) {
                    ResultSet r = conn.createStatement().executeQuery("SELECT `type` FROM `object_map` WHERE uuid=\"" + uuid.toString() + "\"");
                    if (!r.next()) {
                        throw NotExistsSignal.INSTANCE;
                    }
                    return r.getString(1);
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
