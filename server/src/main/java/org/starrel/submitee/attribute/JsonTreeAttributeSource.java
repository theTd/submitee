package org.starrel.submitee.attribute;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;

import javax.swing.plaf.basic.BasicTreeUI;
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

        JsonObject parentNode = getObjectFromPath(parseParentPath(path), false);
        if (parentNode == null) return null;
        JsonElement element = parentNode.get(parsePathNodeName(path));
        if (element == null) return null;
        return serializer.parse(element);
    }

    private static String parseParentPath(String path) {
        if (path.contains(".")) {
            return path.substring(0, path.lastIndexOf("."));
        } else {
            return "";
        }
    }

    private static String parsePathNodeName(String path) {
        if (path.contains(".")) {
            return path.substring(path.lastIndexOf(".") + 1);
        } else {
            return path;
        }
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

        JsonObject parentNode = getObjectFromPath(parseParentPath(path), true);
        assert parentNode != null;
        parentNode.add(parsePathNodeName(path), serializer.write(value));
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

    @Override
    public List<String> listKeys(String path) {
        if (!rootType.equals(Void.class)) throw new RuntimeException("terminal node");

        JsonObject object = getObjectFromPath(path, false);
        if (object == null) return Collections.emptyList();
        return new ArrayList<>(object.keySet());
    }

    @Override
    public void delete(String path) {
        if (path.isEmpty()) {
            jsonRoot = new JsonObject();
            return;
        }
        String parentPath = path.substring(0, path.lastIndexOf("."));
        String nodeName = path.substring(path.lastIndexOf(".") + 1);
        JsonObject object = getObjectFromPath(parentPath, false);
        if (object == null) return;
        object.remove(nodeName);
    }

    @Override
    public <TSubValue> TSubValue getListAttribute(String path, int index, Class<TSubValue> type) {
        AttributeSerializer<TSubValue> serializer = SubmiteeServer.getInstance().getAttributeSerializer(type);
        if (serializer == null) throw new RuntimeException("could not find serializer of type " + type.getName());

        JsonArray array = getArrayFromPath(path, false);
        if (array == null) return null;
        return serializer.parse(array.get(index));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void setListAttribute(String path, int index, Object value) {
        AttributeSerializer serializer = SubmiteeServer.getInstance().getAttributeSerializer(value.getClass());
        if (serializer == null)
            throw new RuntimeException("could not find serializer of type " + value.getClass().getName());

        JsonArray array = getArrayFromPath(path, true);
        assert array != null;
        array.set(index, serializer.write(value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void addListAttribute(String path, Object value) {
        AttributeSerializer serializer = SubmiteeServer.getInstance().getAttributeSerializer(value.getClass());
        if (serializer == null)
            throw new RuntimeException("could not find serializer of type " + value.getClass().getName());

        JsonArray array = getArrayFromPath(path, true);
        assert array != null;
        array.add(serializer.write(value));
    }

    JsonObject getObjectFromPath(String path, boolean createParentNode) {
        if (path.isEmpty()) return jsonRoot.getAsJsonObject();
        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = jsonRoot.getAsJsonObject();
        StringBuilder pathTrace = new StringBuilder();

        while (pathIte.hasNext()) {
            String nodeName = pathIte.next();
            pathTrace.append(nodeName);
            JsonElement node = currentNode.get(nodeName);

            if (node == null || node.isJsonNull()) {
                if (createParentNode) {
                    node = new JsonObject();
                    currentNode.add(nodeName, node);
                } else {
                    return null;
                }
            } else if (!node.isJsonObject()) {
                throw new RuntimeException(pathTrace + " is not json object");
            }
            if (!pathIte.hasNext()) {
                return node.getAsJsonObject();
            } else {
                currentNode = node.getAsJsonObject();
                pathTrace.append(".");
            }
        }
        throw new RuntimeException("empty path?");
    }

    JsonArray getArrayFromPath(String path, boolean createParentNode) {
        Iterator<String> pathIte = Arrays.stream(path.split("\\.")).iterator();
        JsonObject currentNode = jsonRoot.getAsJsonObject();
        StringBuilder pathTrace = new StringBuilder();

        while (pathIte.hasNext()) {
            String nodeName = pathIte.next();
            pathTrace.append(nodeName);
            JsonElement node = currentNode.get(nodeName);

            if (node == null || node.isJsonNull()) {
                if (!pathIte.hasNext()) {
                    node = new JsonArray();
                    currentNode.add(nodeName, node);
                    return node.getAsJsonArray();
                } else if (createParentNode) {
                    node = new JsonObject();
                    currentNode.add(nodeName, node);
                } else {
                    return null;
                }
            } else if (!node.isJsonObject()) {
                throw new RuntimeException(pathTrace + " is not json object");
            }

            if (!pathIte.hasNext()) {
                return node.getAsJsonArray();
            } else {
                currentNode = node.getAsJsonObject();
                pathTrace.append(".");
            }
        }
        throw new RuntimeException("empty path?");
    }

    @Override
    public <TSubValue> List<TSubValue> getListAttributes(String path, Class<TSubValue> type) {
        //noinspection DuplicatedCode
        AttributeSerializer<TSubValue> serializer = SubmiteeServer.getInstance().getAttributeSerializer(type);
        if (serializer == null) throw new RuntimeException("could not find serializer of type " + type.getName());

        JsonArray array = getArrayFromPath(path, false);
        if (array == null) return Collections.emptyList();

        List<TSubValue> list = new ArrayList<>();
        for (JsonElement jsonElement : array) {
            list.add(serializer.parse(jsonElement));
        }
        return list;
    }
}
