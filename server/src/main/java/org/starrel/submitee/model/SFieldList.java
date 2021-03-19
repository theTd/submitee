package org.starrel.submitee.model;

import com.google.gson.JsonArray;
import org.starrel.submitee.SubmiteeServer;

import java.util.List;

public abstract class SFieldList implements SField<List<String>> {
    @Override
    public List<String> parse(String json) {
        return null;
    }

    @Override
    public JsonArray write(List<String> strings) {
        return SubmiteeServer.GSON.toJsonTree(strings).getAsJsonArray();
    }
}
