package org.starrel.submitee.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.starrel.submitee.JsonUtil;
import org.starrel.submitee.SubmiteeServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SubmissionKeeper {
    private final Cache<UUID, SubmissionImpl> cache = CacheBuilder.newBuilder()
            .maximumSize(1000).expireAfterAccess(1, TimeUnit.MINUTES).build();

    private MongoDatabase mongoDatabase;

    public void init() {
        mongoDatabase = SubmiteeServer.getInstance().getMongoDatabase();
    }

    public List<UUID> getSubmissionUUIDs(Bson filters) {
        List<UUID> ids = new ArrayList<>();
        MongoCursor<Document> r = mongoDatabase.getCollection(Submission.ATTRIBUTE_COLLECTION_NAME).find(filters)
                .projection(Projections.include("id")).cursor();
        while (r.hasNext()) {
            ids.add(UUID.fromString(r.next().getString("id")));
        }
        return ids;
    }

    public List<SubmissionImpl> getSubmissions(Bson filters) throws ExecutionException {
        List<SubmissionImpl> result = new ArrayList<>();
        for (UUID uuid : getSubmissionUUIDs(filters)) {
            result.add(getSubmission(uuid));
        }
        return result;
    }

    public SubmissionImpl getSubmission(UUID uuid) throws ExecutionException {
        try {
            return cache.get(uuid, () -> {
                Document document = mongoDatabase.getCollection(Submission.ATTRIBUTE_COLLECTION_NAME)
                        .find(Filters.eq("id", uuid.toString())).first();
                if (document == null) throw NotExistsSignal.INSTANCE;
                JsonElement jsonElement = JsonParser.parseString(document.toJson());
                JsonObject body = JsonUtil.parseObject(jsonElement, "body");
                SubmissionImpl access = new SubmissionImpl(uuid);
                access.getAttributeMap().set("", body);
                return access;
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NotExistsSignal) {
                return null;
            }
            throw e;
        }
    }

    public SubmissionImpl create(UserDescriptor userDescriptor, STemplate template) {
        SubmissionImpl created = new SubmissionImpl(userDescriptor, template);
        cache.put(created.getUniqueId(), created);
        return created;
    }
}
