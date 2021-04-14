package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class AttributeMapImpl<TContext extends AttributeHolder<?>> extends AttributeSpecImpl<Void> implements AttributeMap<TContext> {
    private final TContext holder;
    private boolean autoSaveAttribute;
    private final String collectionName;

    public AttributeMapImpl(TContext holder, String collectionName) {
        super(null, "", Void.class);
        this.holder = holder;
        this.collectionName = collectionName;
        this.autoSaveAttribute = this.collectionName != null;
        setSource(new JsonTreeAttributeSource<>(Void.class));
    }

    @Override
    public TContext getHolder() {
        return holder;
    }

    @Override
    public JsonObject toJsonTree(Predicate<String> pathFilter) {
        JsonObject obj = super.toJsonTree().getAsJsonObject();
        List<String> removes = new ArrayList<>(0);
        for (String path : obj.keySet()) {
            if (!pathFilter.test(path)) {
                removes.add(path);
            }
        }
        if (removes.isEmpty()) return obj;
        obj = obj.deepCopy();
        removes.forEach(obj::remove);
        return obj;
    }

    @Override
    public void setAutoSaveAttribute(boolean autoSaveAttribute) {
        if (!this.autoSaveAttribute && autoSaveAttribute) {
            save();
        }
        this.autoSaveAttribute = autoSaveAttribute;
    }

    @Override
    public boolean getAutoSaveAttribute() {
        return this.autoSaveAttribute;
    }

    @Override
    public void save() {
        if (collectionName == null) throw new RuntimeException("this attribute map cannot be saved");
        MongoCollection<Document> collection = SubmiteeServer.getInstance().getMongoDatabase().getCollection(collectionName);
        Document body = Document.parse(SubmiteeServer.GSON.toJson(toJsonTree()));
        if (collection.find(Filters.eq("id", holder.getAttributePersistKey())).first() != null) {
            collection.updateOne(Filters.eq("id", holder.getAttributePersistKey()), Updates.set("body", body));
        } else {
            Document document = new Document();
            document.put("id", holder.getAttributePersistKey());
            document.put("body", body);
            collection.insertOne(document);
        }
    }

    @Override
    public void read() {
        if (collectionName == null) throw new RuntimeException("this attribute map cannot be saved");
        MongoCollection<Document> collection = SubmiteeServer.getInstance().getMongoDatabase().getCollection(collectionName);
        Document found = collection.find(Filters.eq("id", holder.getAttributePersistKey())).first();
        if (found != null) {
            Document body = (Document) found.get("body");
            setAll("", JsonParser.parseString(body.toJson()).getAsJsonObject());
        } else {
            setAll("", new JsonObject());
        }
    }

    @Override
    public void childUpdated(String path) {
        // TODO: 2021-04-13-0013 partial update
        if (autoSaveAttribute) save();
        try {
            holder.attributeUpdated(path);
        } catch (Exception e) {
            ExceptionReporting.report(AttributeMapImpl.class, "invoking attributeUpdated()", e);
        }
    }
}
