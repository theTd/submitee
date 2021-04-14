package org.starrel.submitee.attribute;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class AttributeSpecImpl<TValue> implements AttributeSpec<TValue> {
    private final AttributeSpecImpl<?> parent;
    private final String path;
    private final boolean isList;
    private final Class<TValue> rootType;
    private final Cache<String, AttributeSpecImpl<?>> specCache = CacheBuilder.newBuilder().build();
    private final TreeMap<String, AttributeSpecImpl<?>> specTreeMap =
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

    private String fullPath(String subPath) {
        if (parent == null) return subPath;
        return parent.fullPath(this.path) + (subPath.isEmpty() ? "" : "." + subPath);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @Override
    public <TSubValue> AttributeSpecImpl<TSubValue> of(String path, Class<TSubValue> type) {
        int idx = path.indexOf(".");
        if (idx != -1) {
            return of(path.substring(0, idx), Void.class).of(path.substring(idx + 1), type);
        }

        AttributeSpecImpl<TSubValue> spec = (AttributeSpecImpl<TSubValue>) specCache.get(path,
                () -> new AttributeSpecImpl<>(AttributeSpecImpl.this, path, type));
        if (spec.rootType != type)
            throw new RuntimeException("type mismatch: " + fullPath(path) + ".path got " + spec.rootType + " expected " + type);

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
    public <TListValue> AttributeSpecImpl<TListValue> ofList(String path, Class<TListValue> type) {
        int idx = path.indexOf(".");
        if (idx != -1) {
            return of(path.substring(0, idx), Void.class).of(path.substring(idx + 1), type);
        }

        AttributeSpecImpl<TListValue> spec = (AttributeSpecImpl<TListValue>) specCache.get(path,
                () -> new AttributeSpecImpl<>(AttributeSpecImpl.this, path, type, true));
        if (spec.rootType != type)
            throw new RuntimeException("type mismatch: " + fullPath(path) + ".path got " + spec.rootType + " expected " + type);

        specTreeMap.put(path, spec);
        return spec;
    }

    private AttributeSpec<?> getSpec(String path) {
        if (path.isEmpty()) return this;

        try {
            return specCache.get(path, () -> {
                for (Map.Entry<String, AttributeSpecImpl<?>> pathSpecEntry : specTreeMap.entrySet()) {
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
        return get(path, type, null);
    }

    @Override
    public <TSubValue> TSubValue get(String path, Class<TSubValue> type, TSubValue defaultValue) {
        if (isList) throw new UnsupportedOperationException("not object");

        AttributeSpec<?> spec = getSpec(path);
        TSubValue value;
        if (spec != this) {
            value = spec.get(path.substring(spec.getPath().length()), type);
        } else {
            value = getSource().getAttribute(fullPath(path), type);
        }
        return value == null ? defaultValue : value;
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
//        childUpdated(path);
    }

    @Override
    public void merge(String path, JsonObject jsonObject) {
        // TODO: 2021-04-14-0014
    }

    @Override
    public List<String> getKeys(String path) {
        if (isList) throw new UnsupportedOperationException("not object");

        AttributeSpec<?> spec = getSpec(path);
        if (spec != this) {
            return spec.getKeys(path.substring(spec.getPath().length()));
        } else {
            return getSource().listKeys(fullPath(path));
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
        getSource().addListAttribute(path, tValue);
        childUpdated(path);
    }

    @Override
    public void add(int index, TValue tValue) {
        if (!isList) throw new UnsupportedOperationException("not list");
        getSource().setListAttribute(fullPath(path), index, tValue);
        childUpdated(path);
    }

    @Override
    public List<TValue> getList(Class<TValue> type) {
        if (isList) throw new UnsupportedOperationException("not list");
        return getSource().getListAttributes(fullPath(path), type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <TSubValue> List<TSubValue> getList(String path, Class<TSubValue> type) {
        if (isList) throw new UnsupportedOperationException("not list");
        AttributeSpec<?> spec = getSpec(path);
        if (spec != this) {
            return spec.getList(((Class) type));
        } else {
            return getSource().getListAttributes(fullPath(path), type);
        }
    }

    @Override
    public void delete(String path) {
        getSource().delete(fullPath(path));
        childUpdated(path);
    }

    @Override
    public void childUpdated(String path) {
        if (this.parent != null) {
            parent.childUpdated(this.path + (path.isEmpty() ? "" : "." + path));
            return;
        }
        ExceptionReporting.report(AttributeSpecImpl.class, "unhandled top level child update", "");
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonElement toJsonTree() {
        if (getSource() instanceof JsonTreeAttributeSource) {
            if (isList) {
                return ((JsonTreeAttributeSource<Void>) getSource()).getArrayFromPath(fullPath(""), false);
            }
            if (rootType == Void.class) {
                // is object
                JsonObject external = collectExternalSource(getRootSource());
                JsonObject self = ((JsonTreeAttributeSource<Void>) getSource()).getObjectFromPath(fullPath(""), false);
                if (self == null) self = new JsonObject();
                if (external != null) {
                    // merge
                    for (Map.Entry<String, JsonElement> entry : external.entrySet()) {
                        if (!self.has(entry.getKey())) {
                            self.add(entry.getKey(), entry.getValue());
                        }
                    }
                }
                return self;
            } else {
                // is primitive
                return ((JsonTreeAttributeSource<?>) getSource()).getElementFromPath(fullPath(""));
            }
        } else {
            if (isList) throw new UnsupportedOperationException();
            if (rootType == Void.class) throw new UnsupportedOperationException();
            AttributeSerializer<TValue> serializer = SubmiteeServer.getInstance().getAttributeSerializer(rootType);
            if (serializer == null) {
                ExceptionReporting.report(AttributeSpecImpl.class, "missing serializer", "missing serializer for " + rootType);
                return null;
            }
            return serializer.write(get());
        }
    }

    private AttributeSource getRootSource() {
        return parent == null ? owningSource : parent.getRootSource();
    }

    private JsonObject collectExternalSource(AttributeSource rootSource) {
        JsonObject obj = null;

        for (Map.Entry<String, AttributeSpecImpl<?>> entry : specCache.asMap().entrySet()) {
            if (entry.getValue().rootType != Void.class && entry.getValue().getSource() != rootSource) {
                if (obj == null) obj = new JsonObject();
                obj.add(entry.getKey(), entry.getValue().toJsonTree());
            } else if (entry.getValue() != this) {
                JsonObject sub = entry.getValue().collectExternalSource(rootSource);
                if (sub != null) {
                    if (obj == null) obj = new JsonObject();
                    obj.add(entry.getKey(), sub);
                }
            }
        }
        return obj;
    }
}
