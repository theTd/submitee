package org.starrel.submitee.attribute;

import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public interface AttributeSpec<TValue> {

    Class<TValue> getType();

    String getPath();

    void setSource(AttributeSource controller);

    void addFilter(AttributeFilter<TValue> filter);

    <TSubValue> AttributeSpec<TSubValue> of(String path, Class<TSubValue> type);

    AttributeSpec<Void> of(String path);

    <TSubValue> AttributeSpec<TSubValue> ofList(String path, Class<TSubValue> type);

    default TValue get() {
        return get("", getType());
    }

    <TSubValue> TSubValue get(String path, Class<TSubValue> type);

    default void set(TValue value) throws AttributeFilter.FilterException {
        set("", value);
    }

    void set(String path, Object value) throws AttributeFilter.FilterException;

    void setAll(String path, JsonObject jsonObject);

    TValue get(int index);

    void add(TValue value);

    void add(int index, TValue value);

    List<TValue> getList();

    List<String> getKeys(String path);

    void delete() throws AttributeFilter.FilterException;

    void childUpdated(String path);
}
