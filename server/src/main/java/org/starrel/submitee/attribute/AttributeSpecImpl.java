package org.starrel.submitee.attribute;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import org.starrel.submitee.ExceptionReporting;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class AttributeSpecImpl<TValue> implements AttributeSpec<TValue> {
    private final AttributeSpecImpl<?> parent;
    private final String path;
    private final boolean isList;
    private final Class<TValue> rootType;
    private final Cache<String, AttributeSpec<?>> specCache = CacheBuilder.newBuilder().build();
    private final TreeMap<String, AttributeSpec<?>> specTreeMap =
            new TreeMap<>(Comparator.comparingInt(o -> o.split("\\.").length));

    protected AttributeSource owningSource;
    private String sourcePath = null;
    private AttributeSource foundSource = null;

    public AttributeSpecImpl(AttributeSpecImpl<?> parent, String path, Class<TValue> rootType) {
        this(parent, path, rootType, false);
    }

    public AttributeSpecImpl(AttributeSpecImpl<?> parent, String path, Class<TValue> rootType, boolean isList) {
        this.parent = parent;
        this.path = path;
        this.rootType = rootType;
        this.isList = isList;
    }

    @Override
    public Class<TValue> getType() {
        return rootType;
    }

    @Override
    public String getPath() {
        return path;
    }

    private AttributeSource getSource() {
        // check if founded controller
        if (foundSource != null) {
            return foundSource;
        }

        // start finding controller

        // if this spec owns controller
        if (owningSource != null) {
            foundSource = owningSource;
            sourcePath = "";
            return owningSource;
        }
        // else find controller from upper level
        if (parent == null) throw new RuntimeException("root attribute spec does not own controller");

        foundSource = parent.getSource();
        sourcePath = parent.sourcePath + (parent.sourcePath.isEmpty() ? "" : ".") + parent.getPath();
        return foundSource;
    }

    @Override
    public void setSource(AttributeSource source) {
        this.owningSource = source;
    }

    private String fullPath(String path) {
        if (parent == null) return path;
        return parent.fullPath(this.path) + (path.isEmpty() ? "" : "." + path);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @Override
    public <TSubValue> AttributeSpec<TSubValue> of(String path, Class<TSubValue> type) {
        AttributeSpec<TSubValue> spec = (AttributeSpec<TSubValue>) specCache.get(path,
                () -> new AttributeSpecImpl<>(AttributeSpecImpl.this, path, type));
        specTreeMap.put(path, spec);
        return spec;
    }

    @Override
    public AttributeSpec<Void> of(String path) {
        return of(path, Void.class);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public <TListValue> AttributeSpec<TListValue> ofList(String path, Class<TListValue> type) {
        AttributeSpec<TListValue> spec = (AttributeSpec<TListValue>) specCache.get(path,
                () -> new AttributeSpecImpl<>(AttributeSpecImpl.this, path, type, true));
        specTreeMap.put(path, spec);
        return spec;
    }

    private AttributeSpec<?> getSpec(String path) {
        if (path.isEmpty()) return this;

        try {
            return specCache.get(path, () -> {
                for (Map.Entry<String, AttributeSpec<?>> pathSpecEntry : specTreeMap.entrySet()) {
                    if (path.startsWith(pathSpecEntry.getKey())) {
                        return pathSpecEntry.getValue();
                    }
                }
                return AttributeSpecImpl.this;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <TSubValue> TSubValue get(String path, Class<TSubValue> type) {
        if (isList) throw new UnsupportedOperationException("not object");

        AttributeSpec<?> spec = getSpec(path);
        if (spec != this) {
            return spec.get(path.substring(spec.getPath().length()), type);
        } else {
            return getSource().getAttribute(fullPath(path), type);
        }
    }

    @Override
    public void set(String path, Object value) {
        if (isList) throw new UnsupportedOperationException("not object");

        AttributeSpec<?> spec = getSpec(path);
        if (spec != this) {
            spec.set(path.substring(spec.getPath().length()), value);
        } else {
            getSource().setAttribute(fullPath(path), value);
            childUpdated(path);
        }
    }

    @Override
    public void setAll(String path, JsonObject jsonObject) {
        if (isList) throw new UnsupportedOperationException("not object");
        getSource().setAll(path, jsonObject);
        childUpdated(path);
    }

    @Override
    public List<String> getKeys(String path) {
        if (isList) throw new UnsupportedOperationException("not object");

        AttributeSpec<?> spec = getSpec(path);
        if (spec != this) {
            return spec.getKeys(path.substring(spec.getPath().length()));
        } else {
            return getSource().listKeys(path);
        }
    }

    @Override
    public TValue get(int index) {
        if (!isList) throw new UnsupportedOperationException("not list");
        return getSource().getListAttribute(fullPath(path), index, rootType);
    }

    @Override
    public void add(TValue tValue) {
        if (!isList) throw new UnsupportedOperationException("not list");
        getSource().addListAttribute(fullPath(path), tValue);
        childUpdated(path);
    }

    @Override
    public void add(int index, TValue tValue) {
        if (!isList) throw new UnsupportedOperationException("not list");
        getSource().setListAttribute(fullPath(path), index, tValue);
        childUpdated(path);
    }

    @Override
    public List<TValue> getList() {
        if (isList) throw new UnsupportedOperationException("not list");
        return getSource().getListAttributes(fullPath(path), rootType);
    }

    @Override
    public void delete() {
        getSource().delete(path);
        childUpdated(path);
    }

    @Override
    public void childUpdated(String path) {
        if (this.parent != null) {
            parent.childUpdated(this.path + "." + path);
            return;
        }
        ExceptionReporting.report(AttributeSpecImpl.class, "unhandled top level child update", "");
    }
}
