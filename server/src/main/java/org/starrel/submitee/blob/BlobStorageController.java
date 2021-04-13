package org.starrel.submitee.blob;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.starrel.submitee.ClassifiedErrors;
import org.starrel.submitee.ClassifiedException;
import org.starrel.submitee.ScriptRunner;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.UserDescriptor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BlobStorageController {
    private final SubmiteeServer server;
    private final Cache<Integer, Blob> cache = CacheBuilder.newBuilder().maximumSize(100)
            .expireAfterAccess(10, TimeUnit.MINUTES).build();
    private final Cache<String, Integer> keyCache = CacheBuilder.newBuilder().maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS).build();

    private final Map<String, BlobStorage> storageMap = new HashMap<>();
    private final Map<String, BlobStorageProvider> providerMap = new HashMap<>();

    public BlobStorageController(SubmiteeServer server) {
        this.server = server;
    }

    public void init() throws SQLException {
        createTables();
        initStorages();
    }

    public List<? extends BlobStorageProvider> getProviders() {
        return new ArrayList<>(providerMap.values());
    }

    public List<? extends BlobStorage> getStorages() {
        return new ArrayList<>(storageMap.values());
    }

    public BlobStorage getStorage(String name) {
        return storageMap.get(name);
    }

    private void createTables() throws SQLException {
        try (Connection conn = server.getDataSource().getConnection()) {
            ResultSet r = conn.getMetaData().getTables(null, null, "blob_storages", null);
            if (!r.next()) {
                server.getLogger().info("creating table blob_storages");
                try {
                    new ScriptRunner(conn, true, true)
                            .runScript(new InputStreamReader(getClass().getResourceAsStream("/blob_storages.sql")));
                } catch (Exception e) {
                    throw new RuntimeException("failed creating table blob_storages", e);
                }
            }
            r = conn.getMetaData().getTables(null, null, "blobs", null);
            if (!r.next()) {
                server.getLogger().info("creating table blobs");
                try {
                    new ScriptRunner(conn, true, true)
                            .runScript(new InputStreamReader(getClass().getResourceAsStream("/blobs.sql")));
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
                server.getLogger().info("initializing blob storage: " + type + ":" + name);

                BlobStorageProvider provider = providerMap.get(type);
                if (provider == null) throw new RuntimeException("cannot find blob storage provider: " + type);
                storageMap.put(name, provider.accessStorage(name));
            }
        }
    }

    public void addProvider(BlobStorageProvider provider) {
        providerMap.put(provider.getTypeId(), provider);
    }

    public BlobStorage createStorage(String providerName, String name) throws ClassifiedException {
        if (storageMap.containsKey(name))
            throw new ClassifiedException(ClassifiedErrors.NAME_CONFLICT);
        BlobStorageProvider provider = providerMap.get(providerName);
        BlobStorage created = provider.createNewStorage(name);
        try {
            registerStorage(created);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return created;
    }

    private void registerStorage(BlobStorage storage) throws SQLException {
        try (Connection conn = server.getDataSource().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO blob_storages(type_id, name) VALUES (?,?)");
            stmt.setString(1, storage.getTypeId());
            stmt.setString(2, storage.getName());
            stmt.executeUpdate();
        }
        storageMap.put(storage.getName(), storage);
    }

    public Blob createNewBlob(String storageName, String fileName, String contentType, UserDescriptor uploader) throws IOException, SQLException {
        BlobStorage storage = storageMap.get(storageName);
        if (storage == null) throw new RuntimeException("blob storage not found: " + storageName);

        UUID uniqueId = UUID.randomUUID();
        String key = uniqueId.toString().replace("-", "");
        int blobId;
        try (Connection conn = server.getDataSource().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO blobs(storage_id, blob_key, file_name, content_type, uploader) VALUES((SELECT id FROM blob_storages WHERE type_id=? AND name=?),?,?,?,?)");
            stmt.setString(1, storage.getTypeId());
            stmt.setString(2, storage.getName());
            stmt.setString(3, key);
            stmt.setString(4, fileName);
            stmt.setString(5, contentType);
            stmt.setString(6, uploader.toString());
            stmt.executeUpdate();

            stmt = conn.prepareStatement("SELECT `blob_id`,`create_time` FROM `blobs` WHERE `blob_key`=?");
            stmt.setString(1, key);

            ResultSet r = stmt.executeQuery();
            if (!r.next()) throw new RuntimeException("insertion failed");
            blobId = r.getInt(1);
        }
        return storage.create(blobId, key, fileName, contentType, uploader);
    }

    public Blob getBlobByKey(String blobKey) throws Exception {
        try {
            int id = keyCache.get(blobKey, () -> {
                try (Connection conn = server.getDataSource().getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("SELECT blob_id FROM blobs WHERE blob_key=?");
                    stmt.setString(1, blobKey);
                    ResultSet r = stmt.executeQuery();
                    if (!r.next()) {
                        throw new BlobNotFoundSignal();
                    }
                    return r.getInt(1);
                }
            });
            return access(id);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BlobNotFoundSignal) {
                return null;
            } else {
                throw e;
            }
        }
    }

    public Blob access(int blobId) throws Exception {
        try {
            return cache.get(blobId, () -> {
                try (Connection conn = server.getDataSource().getConnection()) {
                    ResultSet r = conn.createStatement().executeQuery(
                            "SELECT S.type_id, S.name, B.file_name, B.blob_key, " +
                                    "B.create_time, B.content_type, B.uploader " +
                                    "FROM blobs B " +
                                    "RIGHT JOIN blob_storages S " +
                                    "ON S.id = B.storage_id " +
                                    "WHERE B.blob_id =" +
                                    " " + blobId);
                    if (!r.next()) {
                        throw BlobNotFoundSignal.INSTANCE;
                    }
                    String storageType = r.getString(1);
                    String storageName = r.getString(2);
                    String fileName = r.getString(3);
                    String blobKey = r.getString(4);
                    Date createTime = r.getTimestamp(5);
                    String contentType = r.getString(6);
                    UserDescriptor uploader = UserDescriptor.parse(r.getString(7));

                    BlobStorage storage = storageMap.get(storageName);
                    if (storage == null) {
                        throw new NullPointerException("blob storage missing: " + storageType + ":" + storageName);
                    }
                    return storage.access(blobId, blobKey, fileName, createTime, contentType, uploader);
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BlobNotFoundSignal) {
                return null;
            }
            throw e;
        }
    }
}
