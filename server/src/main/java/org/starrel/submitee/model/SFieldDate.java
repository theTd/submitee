package org.starrel.submitee.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.starrel.submitee.SubmiteeServer;

import java.util.Date;

public abstract class SFieldDate implements SField<Date> {
    @Override
    public Date parse(String json) {
        return new Date(JsonParser.parseString(json).getAsJsonObject().getAsLong());
    }

    @Override
    public JsonElement write(Date date) {
        return SubmiteeServer.GSON.toJsonTree(date);
    }
}
