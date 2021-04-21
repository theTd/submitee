package org.starrel.submitee.attribute;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;

import java.util.*;

public class JsonTreeAttributeSource<TValue> implements AttributeSource {
    private final Class<TValue> rootType;
    private JsonObject jsonRoot = new JsonObject();

    public JsonTreeAttributeSource(Class<TValue> rootType) {
        this.rootType = rootType;
    }

    public JsonObject getJsonRoot() {
        return jsonRoot;
    }

    public JsonElement getElementFromPath(String path) {
        if (path.isEmpty()) return jsonRoot;
        JsonObject parent = getObjectFromPath(parseParentPath(path), false);
        if (parent == null) return null;
        return parent.get(parsePathNodeName(path));
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
        if (value == null) {
            delete(path);
            return;
        }

        AttributeSerializer serializer = SubmiteeServer.getInstance().getAttributeSerializer(value.getClass());
        if (serializer == null)
            throw new RuntimeException("could not find serializer of type " + value.getClass().getName());

        // self
        if (path.isEmpty()) {
            JsonElement write = serializer.write(value);
            if (write == null) write = new JsonObject();
            if (!write.isJsonObject()) throw new RuntimeException("setting json root to non json object");
            jsonRoot = write.getAsJsonObject();
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
    public void setAll(String path, JsonObject object) {
        if (path.isEmpty()) {
            this.jsonRoot = object;
            return;
        }

        JsonObject fromPath = getObjectFromPath(parseParentPath(path), true);
        fromPath.add(parsePathNodeName(path), object);
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
        String parentPath = parseParentPath(path);
        String nodeName = parsePathNodeName(path);
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
    public void addListAttribute(String path, Object value) {
        AttributeSerializer serializer = SubmiteeServer.getInstance().getAttributeSerializer(value.getClass());
        if (serializer == null)
            throw new RuntimeException("could not find serializer of type " + value.getClass().getName());

        JsonArray array = getArrayFromPath(path, true);
        assert array != null;
        array.add(serializer.write(value));
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
    public void addListAttribute(String path, int index, Object value) {
        AttributeSerializer serializer = SubmiteeServer.getInstance().getAttributeSerializer(value.getClass());
        if (serializer == null)
            throw new RuntimeException("could not find serializer of type " + value.getClass().getName());

        JsonElement add = serializer.write(value);

        JsonArray array = getArrayFromPath(path, true);
        assert array != null;
        if (index >= array.size()) {
            array.add(add);
        } else {
            // insert
            array.add(array.get(array.size() - 1));
            int cur = array.size();
            while (--cur > index) {
                array.set(cur, array.get(cur - 1));
            }
            array.set(index, add);
        }
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
            }

            if (!pathIte.hasNext()) {
                return node.getAsJsonArray();
            } else {
                if (!node.isJsonObject()) {
                    throw new RuntimeException(pathTrace + " is not json object");
                }
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
