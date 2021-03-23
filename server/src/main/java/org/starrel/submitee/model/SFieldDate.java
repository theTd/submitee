package org.starrel.submitee.model;

import com.google.gson.JsonElement;
import org.starrel.submitee.SubmiteeServer;

import java.util.Date;

public abstract class SFieldDate implements SField<Date> {
    @Override
    public Date parse(JsonElement json) {
        return new Date(json.getAsLong());
    }

    @Override
    public JsonElement write(Date date) {
        return SubmiteeServer.GSON.toJsonTree(date);
    }
}
