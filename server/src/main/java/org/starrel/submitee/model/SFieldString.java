package org.starrel.submitee.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.starrel.submitee.SubmiteeServer;

public abstract class SFieldString implements SField<String> {
    @Override
    public String parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject().getAsString();
    }

    @Override
    public JsonElement write(String s) {
        return SubmiteeServer.GSON.toJsonTree(s);
    }
}
