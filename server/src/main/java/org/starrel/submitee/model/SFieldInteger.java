package org.starrel.submitee.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.starrel.submitee.SubmiteeServer;

public abstract class SFieldInteger implements SField<Integer> {
    @Override
    public Integer parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject().getAsInt();
    }

    @Override
    public JsonElement write(Integer integer) {
        return SubmiteeServer.GSON.toJsonTree(integer);
    }
}
