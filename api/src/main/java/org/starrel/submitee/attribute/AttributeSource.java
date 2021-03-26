package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;

import java.util.List;

public interface AttributeSource {

    <TValue> TValue getAttribute(String path, Class<TValue> type);

    void setAttribute(String path, Object value);

    default void setAll(String path, JsonObject object) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("cannot use setAll on " + getClass().getName());
    }

    List<String> listKeys(String path);

    void delete(String path);
}
