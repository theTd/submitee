package org.starrel.submitee.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.starrel.submitee.SubmiteeServer;

import java.util.ArrayList;
import java.util.List;

public abstract class SFieldList implements SField<List<String>> {
    @Override
    public List<String> parse(JsonElement json) {
        List<String> list = new ArrayList<>();
        for (JsonElement jsonElement : json.getAsJsonArray()) {
            list.add(jsonElement.getAsString());
        }
        return list;
    }

    @Override
    public JsonArray write(List<String> strings) {
        return SubmiteeServer.GSON.toJsonTree(strings).getAsJsonArray();
    }
}
