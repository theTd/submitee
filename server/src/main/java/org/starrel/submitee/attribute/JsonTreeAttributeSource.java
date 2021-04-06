package org.starrel.submitee.attribute;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;

import java.util.*;

public class JsonTreeAttributeSource<TValue> implements AttributeSource {
    private final Class<TValue> rootType;
    private JsonElement jsonRoot;

    public JsonTreeAttributeSource(Class<TValue> rootType) {
        this.rootType = rootType;
    }

    public JsonElement getJsonRoot() {
        return jsonRoot;
    }

    @Override
    public <TSubValue> TSubValue getAttribute(String path, Class<TSubValue> type) {
        AttributeSerializer<TSubValue> serializer = SubmiteeServer.getInstance().getAttributeSerializer(type);
        if (serializer == null) throw new RuntimeException("could not find serializer of type " + type.getName());

        // self
        if (path.isEmpty()) {
            if (jsonRoot == null) return null;
            return serializer.parse(jsonRoot);
        }

        // child
        if (!rootType.equals(Void.class)) {
            throw new RuntimeException("terminal node");
        }

        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = jsonRoot.getAsJsonObject();
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
            JsonElement nodeElement = currentNode.get(node);
            if (nodeElement == null) return null;
            return serializer.parse(nodeElement);
        }
        throw new RuntimeException("empty path");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void setAttribute(String path, Object value) {
        AttributeSerializer serializer = SubmiteeServer.getInstance().getAttributeSerializer(value.getClass());
        if (serializer == null)
            throw new RuntimeException("could not find serializer of type " + value.getClass().getName());

        // self
        if (path.isEmpty()) {
            jsonRoot = serializer.write(value);
            return;
        }

        if (!rootType.equals(Void.class)) {
            throw new RuntimeException("terminal node");
        }

        if (jsonRoot == null) jsonRoot = new JsonObject();

        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = jsonRoot.getAsJsonObject();
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
                if (currentNode.has(node)) {
                    currentNode.remove(node);
                }
                currentNode.add(node, serializer.write(value));
            }
        }
    }

    @Override
    public void setAll(String path, JsonObject object) throws UnsupportedOperationException {
        if (path.contains(".")) throw new IllegalArgumentException("cannot use high level path on setAll()");
        // TODO: 2021-03-26-0026
        if (jsonRoot == null) {
            jsonRoot = new JsonObject();
        }
        if (path.isEmpty()) {
            jsonRoot = object;
        } else {
            jsonRoot.getAsJsonObject().add(path, object);
        }
    }

    private JsonObject getNode(String path) {
        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = jsonRoot.getAsJsonObject();
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
    public List<String> listKeys(String path) {
        if (!rootType.equals(Void.class)) throw new RuntimeException("terminal node");
        JsonObject node = getNode(path);
        if (node == null) return Collections.emptyList();
        return new ArrayList<>(node.keySet());
    }

    @Override
    public void delete(String path) {
        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = jsonRoot.getAsJsonObject();
        JsonElement temp;
        while (pathIte.hasNext()) {
            String node = pathIte.next();
            if (!pathIte.hasNext()) {
                // delete in this node
                currentNode.remove(node);
                return;
            } else {
                if (currentNode.has(node) && (temp = currentNode.get(node)).isJsonObject()) {
                    currentNode = temp.getAsJsonObject();
                } else {
                    // ended not found target node
                    return;
                }
            }
        }
    }
}
