package org.starrel.submitee.model;

import com.google.gson.JsonElement;
import org.starrel.submitee.SubmiteeServer;

public abstract class SFieldInteger implements SField<Integer> {
    @Override
    public Integer parse(JsonElement json) {
        return json.getAsInt();
    }

    @Override
    public JsonElement write(Integer integer) {
        return SubmiteeServer.GSON.toJsonTree(integer);
    }
}
