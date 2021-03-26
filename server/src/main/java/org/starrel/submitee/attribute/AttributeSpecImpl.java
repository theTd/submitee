package org.starrel.submitee.attribute;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import org.starrel.submitee.ExceptionReporting;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class AttributeSpecImpl<TValue> implements AttributeSpec<TValue> {
    private final AttributeSpecImpl<?> parent;
    private final String path;
    private final Class<TValue> rootType;
    private final Cache<String, AttributeSpec<?>> specCache = CacheBuilder.newBuilder().build();
    private final TreeMap<String, AttributeSpec<?>> specTreeMap =
            new TreeMap<>(Comparator.comparingInt(o -> o.split("\\.").length));

    private final List<AttributeFilter<TValue>> filters = Collections.synchronizedList(new LinkedList<>());

    protected AttributeSource owningSource;
    private String sourcePath = null;
    private AttributeSource foundSource = null;

    public AttributeSpecImpl(AttributeSpecImpl<?> parent, String path, Class<TValue> rootType) {
        this.parent = parent;
        this.path = path;
        this.rootType = rootType;
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

    @Override
    public void addFilter(AttributeFilter<TValue> filter) {
        this.filters.add(filter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TSubValue> AttributeSpec<TSubValue> of(String path, Class<TSubValue> type) {
        try {
            return (AttributeSpec<TSubValue>) specTreeMap.put(path, specCache.get(path,
                    () -> new AttributeSpecImpl<>(AttributeSpecImpl.this, path, type)));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AttributeSpec<Void> of(String path) {
        return of(path, Void.class);
    }

    private AttributeSpec<?> getSpec(String path) {
        if (path.isEmpty()) return this;

        try {
            return specCache.get(path, () -> {
                for (Map.Entry<String, AttributeSpec<?>> pathSpecEntry : specTreeMap.entrySet()) {
                    if (pathSpecEntry.getKey().startsWith(path)) return pathSpecEntry.getValue();
                }
                return AttributeSpecImpl.this;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <TSubValue> TSubValue get(String path, Class<TSubValue> type) {
        AttributeSpec<?> spec = getSpec(path);
        if (spec != this) {
            return spec.get(path.substring(spec.getPath().length()), type);
        } else {
            return getSource().getAttribute(sourcePath, type);
        }
    }

    @Override
    public void set(String path, Object value) throws AttributeFilter.FilterException {
        AttributeSpec<?> spec = getSpec(path);
        if (spec != this) {
            spec.set(path.substring(spec.getPath().length()), value);
        } else {
            if (path.isEmpty()) {
                for (AttributeFilter<TValue> filter : filters) {
                    //noinspection unchecked
                    filter.onSet((TValue) value);
                }
            } else {
                for (AttributeFilter<TValue> filter : filters) {
                    filter.onSet(path, value);
                }
            }

            getSource().setAttribute(path, value);

            childUpdated(path);
        }
    }

    @Override
    public void setAll(String path, JsonObject jsonObject) {
        getSource().setAll(path, jsonObject);
    }

    @Override
    public List<String> getKeys(String path) {
        AttributeSpec<?> spec = getSpec(path);
        if (spec != this) {
            return spec.getKeys(path.substring(spec.getPath().length()));
        } else {
            return getSource().listKeys(path);
        }
    }

    @Override
    public void delete() throws AttributeFilter.FilterException {
        for (AttributeFilter<TValue> filter : filters) {
            filter.onDelete("");
        }
        getSource().delete(path);
    }

    @Override
    public void childUpdated(String path) {
        if (this.parent != null) {
            parent.childUpdated(this.path + "." + path);
            return;
        }
        ExceptionReporting.report("unhandled top level child update", new Throwable());
    }
}
