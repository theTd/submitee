package org.starrel.submitee.attribute;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;

import java.util.*;

public class AttributeMapImpl<TContext extends AttributeHolder<?>> implements AttributeMap<TContext> {
    private final TContext holder;
    private final JsonObject root;

    public AttributeMapImpl(TContext holder) {
        this.holder = holder;
        this.root = new JsonObject();
    }

    @Override
    public TContext getHolder() {
        return holder;
    }

    @Override
    public <TValue> TValue getAttribute(String path, Class<TValue> type) {
        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = root;
        JsonElement temp;
        StringBuilder pathTrace = new StringBuilder();
        while (pathIte.hasNext()) {
            String node = pathIte.next();
            pathTrace.append(node);

            if (pathIte.hasNext()) {
                if (currentNode.has(node)) {
                    temp = currentNode.get(node);
                    if (!temp.isJsonObject()) throw new RuntimeException(pathTrace + "is not an json object");
                    currentNode = temp.getAsJsonObject();
                    continue;
                } else {
                    currentNode.add(node, currentNode = new JsonObject());
                }
                pathTrace.append(".");
                continue;
            }
            return SubmiteeServer.GSON.fromJson(currentNode.get(node), type);
        }
        throw new RuntimeException("empty path");
    }

    @Override
    public List<String> getKeys(String path) {
        JsonObject node = getNode(path);
        if (node == null) return Collections.emptyList();
        return new ArrayList<>(node.keySet());
    }

    private JsonObject getNode(String path) {
        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = root;
        JsonElement temp;
        while (pathIte.hasNext()) {
            String node = pathIte.next();
            if (!currentNode.has(node)) return null;
            if (!(temp = currentNode.get(node)).isJsonObject()) return null;
            currentNode = temp.getAsJsonObject();
        }
        return currentNode;
    }

    @Override
    public void setAttribute(String path, Object value) {
        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = root;
        JsonElement temp;
        StringBuilder pathTrace = new StringBuilder();
        while (pathIte.hasNext()) {
            String node = pathIte.next();
            pathTrace.append(node);

            if (pathIte.hasNext()) {
                if (currentNode.has(node)) {
                    temp = currentNode.get(node);
                    if (!temp.isJsonObject()) throw new RuntimeException(pathTrace + "is not an json object");
                    currentNode = temp.getAsJsonObject();
                    continue;
                } else {
                    currentNode.add(node, currentNode = new JsonObject());
                }
                pathTrace.append(".");
            } else {
                currentNode.remove(node);
                currentNode.add(node, SubmiteeServer.GSON.toJsonTree(value));
            }
        }
        holder.attributeUpdated(path);
    }

    @Override
    public boolean removeAttribute(String path) {
        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = root;
        JsonElement temp;
        while (pathIte.hasNext()) {
            String node = pathIte.next();
            if (pathIte.hasNext()) {
                if (currentNode.has(node)) {
                    temp = currentNode.get(node);
                    if (!temp.isJsonObject()) {
                        return false;
                    }
                    currentNode = temp.getAsJsonObject();
                } else {
                    return false;
                }
            } else {
                if (currentNode.has(node)) {
                    currentNode.remove(node);
                } else {
                    return false;
                }
            }
        }
        try {
            holder.attributeUpdated(path);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return true;
    }

    @Override
    public JsonObject serialize() {
        return root.deepCopy();
    }
}
