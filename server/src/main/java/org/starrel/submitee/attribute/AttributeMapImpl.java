package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.starrel.submitee.SubmiteeServer;

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

    @SuppressWarnings("unchecked")
    @Override
    public JsonObject serialize() {
        return ((JsonTreeAttributeSource<Void>) owningSource).getJsonRoot().getAsJsonObject();
    }

    @Override
    public void setAutoSaveAttribute(boolean autoSaveAttribute) {
        this.autoSaveAttribute = autoSaveAttribute;
    }

    @Override
    public boolean getAutoSaveAttribute() {
        return this.autoSaveAttribute;
    }

    @Override
    public void saveAttribute(MongoDatabase mongoDatabase) {
        if (collectionName == null) throw new RuntimeException("this attribute map cannot be saved");
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Document body = Document.parse(SubmiteeServer.GSON.toJson(serialize()));
        Document document = new Document();
        document.put("id", holder.getAttributePersistKey());
        document.put("body", body);
        if (collection.find(Filters.eq("id", holder.getAttributePersistKey())).first() != null) {
            collection.updateOne(Filters.eq("id", holder.getAttributePersistKey()), Updates.set("body", body));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public void readAttribute(MongoDatabase mongoDatabase) {
        if (collectionName == null) throw new RuntimeException("this attribute map cannot be saved");
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Document found = collection.find(Filters.eq("id", holder.getAttributePersistKey())).first();
        if (found != null) {
            Document body = (Document) found.get("body");
            setAll("", JsonParser.parseString(body.toJson()).getAsJsonObject());
        }
    }

    @Override
    public void childUpdated(String path) {
        if (autoSaveAttribute) {
            saveAttribute(SubmiteeServer.getInstance().getMongoDatabase());
        }
    }
}
