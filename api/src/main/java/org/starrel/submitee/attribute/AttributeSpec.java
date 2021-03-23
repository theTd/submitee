package org.starrel.submitee.attribute;

import java.util.List;

public interface AttributeSpec<TValue> {

    Class<TValue> getType();

    String getPath();

    void setSource(AttributeSource controller);

    <TValue> AttributeSpec<TValue> of(String path, Class<TValue> type);

    AttributeSpec<Void> of(String path);

    default TValue get() {
        return get("", getType());
    }

    <TSubValue> TSubValue get(String path, Class<TSubValue> type);

    default void set(TValue value) {
        set("", value);
    }

    void set(String path, Object value);

    List<String> getKeys(String path);

    void delete();

    void childUpdated(String path);
}
