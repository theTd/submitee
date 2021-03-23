package org.starrel.submitee.attribute;

import com.google.gson.JsonElement;

public abstract class AttributeSerializer<TValue> {
    private final Class<TValue> valueType;

    public AttributeSerializer(Class<TValue> valueType) {
        this.valueType = valueType;
    }

    public abstract TValue parse(JsonElement json);

    public abstract JsonElement write(TValue value);

}
