package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;

import java.util.List;

public interface AttributeSpec<TValue> {

    Class<TValue> getType();

    String getPath();

    void setSource(AttributeSource controller);

    <TSubValue> AttributeSpec<TSubValue> of(String path, Class<TSubValue> type);

    AttributeSpec<Void> of(String path);

    <TSubValue> AttributeSpec<TSubValue> ofList(String path, Class<TSubValue> type);

    default TValue get() {
        return get("", getType());
    }

    <TSubValue> TSubValue get(String path, Class<TSubValue> type);

    <TSubValue> TSubValue get(String path, Class<TSubValue> type, TSubValue defaultValue);

    default void set(TValue value) {
        set("", value);
    }

    void set(String path, Object value);

    void setAll(String path, JsonObject jsonObject);

    TValue get(int index);

    void add(TValue value);

    void add(int index, TValue value);

    default List<TValue> getList() {
        return getList("", getType());
    }

    List<TValue> getList(Class<TValue> type);

    <TSubValue> List<TSubValue> getList(String path, Class<TSubValue> type);

    List<String> getKeys(String path);

    void delete();

    void childUpdated(String path);
}
