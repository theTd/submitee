package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;
import com.mongodb.client.MongoDatabase;

import java.util.function.Predicate;

public interface AttributeMap<TContext extends AttributeHolder<?>> extends AttributeSpec<Void> {

    TContext getHolder();

    JsonObject toJsonTree(Predicate<String> pathFilter);

    JsonObject toJsonTree();

    void setAutoSaveAttribute(boolean autoSaveAttribute);

    boolean getAutoSaveAttribute();

    void saveAttribute(MongoDatabase mongoDatabase);

    void readAttribute(MongoDatabase mongoDatabase);

}
