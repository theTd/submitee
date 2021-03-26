package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;
import com.mongodb.client.MongoDatabase;

public interface AttributeMap<TContext extends AttributeHolder<?>> extends AttributeSpec<Void> {

    TContext getHolder();

    JsonObject serialize();

    void setAutoSaveAttribute(boolean autoSaveAttribute);

    boolean getAutoSaveAttribute();

    void saveAttribute(MongoDatabase mongoDatabase);

    void readAttribute(MongoDatabase mongoDatabase);

}
