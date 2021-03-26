package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;

import java.util.List;

public interface AttributeSpec<TValue> {

    Class<TValue> getType();

    String getPath();

    void setSource(AttributeSource controller);

    void addFilter(AttributeFilter<TValue> filter);

    <TValue> AttributeSpec<TValue> of(String path, Class<TValue> type);

    AttributeSpec<Void> of(String path);

    default TValue get() {
        return get("", getType());
    }

    <TSubValue> TSubValue get(String path, Class<TSubValue> type);

    default void set(TValue value) throws AttributeFilter.FilterException {
        set("", value);
    }

    void set(String path, Object value) throws AttributeFilter.FilterException;

    void setAll(String path, JsonObject jsonObject);

    List<String> getKeys(String path);

    void delete() throws AttributeFilter.FilterException;

    void childUpdated(String path);
}
