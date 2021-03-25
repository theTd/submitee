package org.starrel.submitee.model;

import com.google.gson.JsonElement;
import org.starrel.submitee.SubmiteeServer;

public abstract class SFieldString implements SField<String> {
    @Override
    public String parse(JsonElement json) {
        return json.getAsString();
    }

    @Override
    public JsonElement write(String s) {
        return SubmiteeServer.GSON.toJsonTree(s);
    }
}
