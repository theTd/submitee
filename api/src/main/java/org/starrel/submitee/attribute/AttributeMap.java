package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;

import java.util.List;

public interface AttributeMap<TContext extends AttributeHolder<?>> {

    TContext getHolder();

    <TValue> TValue getAttribute(String path, Class<TValue> type);

    void setAttribute(String path, Object value);

    boolean removeAttribute(String path);

    List<String> getKeys(String path);

    JsonObject serialize();

}
