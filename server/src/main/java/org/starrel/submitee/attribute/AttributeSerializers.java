package org.starrel.submitee.attribute;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.UserDescriptor;

import java.util.Date;

public abstract class AttributeSerializers {
    public static AttributeSerializer<String> STRING = new AttributeSerializer<String>() {
        @Override
        public String parse(JsonElement json) {
            return json.getAsString();
        }

        @Override
        public JsonElement write(String s) {
            return SubmiteeServer.GSON.toJsonTree(s);
        }
    };

    public static AttributeSerializer<Integer> INTEGER = new AttributeSerializer<Integer>() {
        @Override
        public Integer parse(JsonElement json) {
            return json.getAsInt();
        }

        @Override
        public JsonElement write(Integer integer) {
            return SubmiteeServer.GSON.toJsonTree(integer);
        }
    };

    public static AttributeSerializer<Double> DOUBLE = new AttributeSerializer<Double>() {
        @Override
        public Double parse(JsonElement json) {
            return json.getAsDouble();
        }

        @Override
        public JsonElement write(Double aDouble) {
            return SubmiteeServer.GSON.toJsonTree(aDouble);
        }
    };

    public static AttributeSerializer<Boolean> BOOLEAN = new AttributeSerializer<Boolean>() {
        @Override
        public Boolean parse(JsonElement json) {
            return json.getAsBoolean();
        }

        @Override
        public JsonElement write(Boolean aBoolean) {
            return SubmiteeServer.GSON.toJsonTree(aBoolean);
        }
    };

    public static AttributeSerializer<Date> DATE = new AttributeSerializer<Date>() {
        @Override
        public Date parse(JsonElement json) {
            return new Date(json.getAsLong());
        }

        @Override
        public JsonElement write(Date date) {
            return SubmiteeServer.GSON.toJsonTree(date.getTime());
        }
    };
}
