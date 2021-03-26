package org.starrel.submitee.attribute;

import com.google.gson.JsonElement;

public abstract class AttributeSerializer<TValue> {

    public abstract TValue parse(JsonElement json);

    public abstract JsonElement write(TValue value);

}
