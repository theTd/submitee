package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;
import com.mongodb.client.MongoDatabase;

import java.util.function.Predicate;

public interface AttributeMap<TContext extends AttributeHolder<?>> extends AttributeSpec<Void> {

    TContext getHolder();

    JsonObject toJson(Predicate<String> pathFilter);

    JsonObject toJson();

    void setAutoSaveAttribute(boolean autoSaveAttribute);

    boolean getAutoSaveAttribute();

    void saveAttribute(MongoDatabase mongoDatabase);

    void readAttribute(MongoDatabase mongoDatabase);

}
