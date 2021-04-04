package org.starrel.submitee.blob;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.starrel.submitee.ScriptRunner;
import org.starrel.submitee.SubmiteeServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BlobStorageController {
    private final SubmiteeServer server;
    private final Cache<Integer, Blob> cache = CacheBuilder.newBuilder().maximumSize(1000)
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

    private final Map<String, BlobStorage> storageMap = new HashMap<>();
    private final Map<String, BlobStorageProvider> providerMap = new HashMap<>();

    public BlobStorageController(SubmiteeServer server) {
        this.server = server;
    }

    public void init() throws SQLException {
        createTables();
        initStorages();
    }

    private void createTables() throws SQLException {
        try (Connection conn = server.getDataSource().getConnection()) {
            ResultSet r = conn.getMetaData().getTables(null, null, "blob_storages", null);
            if (!r.next()) {
                server.getLogger().info("creating table blob_storages");
                try {
                    new ScriptRunner(conn, true, true).runScript(new InputStreamReader(getClass().getResourceAsStream("/blob_storages.sql")));
                } catch (Exception e) {
                    throw new RuntimeException("failed creating table blob_storages", e);
                }
            }
            r = conn.getMetaData().getTables(null, null, "blobs", null);
            if (!r.next()) {
                server.getLogger().info("creating table blobs");
                try {
                    new ScriptRunner(conn, true, true).runScript(new InputStreamReader(getClass().getResourceAsStream("/blobs.sql")));
                } catch (Exception e) {
                    throw new RuntimeException("failed creating table blobs", e);
                }
            }
        }
    }

    private void initStorages() throws SQLException {
        try (Connection conn = server.getDataSource().getConnection()) {
            ResultSet r = conn.createStatement().executeQuery("SELECT type_id,name FROM blob_storages");
            while (r.next()) {
                String type = r.getString(1);
                String name = r.getString(2);
                String storageKey = type + ":" + name;
                server.getLogger().info("initializing blob storage: " + storageKey);

                BlobStorageProvider provider = providerMap.get(type);
                if (provider == null) throw new RuntimeException("cannot find blob storage provider: " + type);
                storageMap.put(storageKey, provider.accessStorage(name));
            }
        }
    }

    public void addProvider(BlobStorageProvider provider) {
        providerMap.put(provider.getTypeId(), provider);
    }

    public Blob createNewBlob(String storageKey, String fileName) throws IOException, SQLException {
        BlobStorage storage = storageMap.get(storageKey);
        if (storage == null) throw new RuntimeException("blob storage not found: " + storageKey);

        UUID uniqueId = UUID.randomUUID();
        String key = uniqueId.toString().replace("-", "");
        int blobId;
        try (Connection conn = server.getDataSource().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO blobs(storage_id, blob_key, file_name) VALUES((SELECT id FROM blob_storages WHERE type_id=? AND name=?),?,?)");
            stmt.setString(1, storage.getTypeId());
            stmt.setString(2, storage.getName());
            stmt.setString(3, key);
            stmt.setString(4, fileName);
            stmt.executeUpdate();
            ResultSet r = conn.createStatement().executeQuery("SELECT `blob_id`,`create_time` FROM `blobs` WHERE `blob_key`=" + key);
            if (!r.next()) throw new RuntimeException("insertion failed");
            blobId = r.getInt(1);
        }
        return storage.create(blobId, key, fileName);
    }

    public Blob access(int blobId) throws Exception {
        try {
            return cache.get(blobId, () -> {
                try (Connection conn = server.getDataSource().getConnection()) {
                    ResultSet r = conn.createStatement().executeQuery(
                            "SELECT (SELECT type_id AS storage_type, name FROM blob_storages AS storage_name " +
                                    "WHERE blob_storages.id=blobs.storage_id),file_name,create_time FROM blobs WHERE blob_id=" + blobId);
                    if (r.next()) {
                        String type = r.getString(1);
                        String type_name = r.getString(2);
                        String key = r.getString(3);
                        String fileName = r.getString(4);
                        Date createTime = r.getTimestamp(5);
                        String storageKey = type + ":" + type_name;
                        BlobStorage storage = storageMap.get(storageKey);
                        if (storage == null) {
                            throw new NullPointerException("blob storage missing: " + storageKey);
                        }
                        return storage.access(blobId, key, fileName, createTime);
                    } else {
                        return null;
                    }
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception) {
                throw ((Exception) e.getCause());
            }
            throw new Error(e.getCause());
        }
    }
}
