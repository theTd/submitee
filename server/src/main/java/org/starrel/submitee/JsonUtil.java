package org.starrel.submitee;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public abstract class JsonUtil {
    public static JsonPrimitive parsePrimitive(JsonElement element, String propertyName) {
        if (element == null) return null;
        if (!element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has(propertyName)) return null;
        JsonElement e = obj.get(propertyName);
        if (!e.isJsonPrimitive()) return null;
        return e.getAsJsonPrimitive();
    }

    public static Boolean parseBoolean(JsonElement element, String propertyName) {
        JsonPrimitive p = parsePrimitive(element, propertyName);
        return p == null ? null : p.getAsBoolean();
    }

    public static String parseString(JsonElement element, String propertyName) {
        JsonPrimitive p = parsePrimitive(element, propertyName);
        return p == null ? null : p.isString() ? p.getAsString() : null;
    }

    public static Integer parseInt(JsonElement element, String propertyName) {
        JsonPrimitive p = parsePrimitive(element, propertyName);
        return p == null ? null : p.isNumber() ? p.getAsInt() : null;
    }

    public static Long parseLong(JsonElement element, String propertyName) {
        JsonPrimitive p = parsePrimitive(element, propertyName);
        return p == null ? null : p.isNumber() ? p.getAsLong() : null;
    }

    public static Double parseDouble(JsonElement element, String propertyName) {
        JsonPrimitive p = parsePrimitive(element, propertyName);
        return p == null ? null : p.isNumber() ? p.getAsDouble() : null;
    }

    public static JsonObject parseObject(JsonElement element, String propertyName) {
        if (element == null) return null;
        if (!element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has(propertyName)) return null;
        JsonElement ele = obj.get(propertyName);
        if (!ele.isJsonObject()) return null;
        return ele.getAsJsonObject();
    }

    public static JsonArray parseArray(JsonElement element, String propertyName) {
        if (element == null) return null;
        if (!element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has(propertyName)) return null;
        JsonElement ele = obj.get(propertyName);
        if (!ele.isJsonArray()) return null;
        return ele.getAsJsonArray();
    }
}
