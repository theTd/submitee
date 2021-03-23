package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;

import java.util.List;

public interface AttributeMap<TContext extends AttributeHolder<?>> extends AttributeSpec<Void> {

    TContext getHolder();

    JsonObject serialize();

}
