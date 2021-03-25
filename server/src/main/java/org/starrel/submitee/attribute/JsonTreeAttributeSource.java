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

        // self
        if (path.isEmpty()) {
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
            return serializer.parse(currentNode.get(node));
        }
        throw new RuntimeException("empty path");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void setAttribute(String path, Object value) {
        AttributeSerializer serializer = SubmiteeServer.getInstance().getAttributeSerializer(value.getClass());

        // self
        if (path.isEmpty()) {
            jsonRoot = serializer.write(value);
            return;
        }

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
            } else {
                currentNode.remove(node);
                currentNode.add(node, serializer.write(value));
            }
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
}
