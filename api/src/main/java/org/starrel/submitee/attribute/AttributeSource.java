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

    default void specSet(String path) {
    }

    void delete(String path);

    <TValue> TValue getListAttribute(String path, int index, Class<TValue> type);

    void setListAttribute(String path, int index, Object tValue);

    void addListAttribute(String path, Object value);

    void addListAttribute(String path, int index, Object value);

    <TValue> List<TValue> getListAttributes(String path, Class<TValue> type);
}
