package org.starrel.submitee.attribute;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.starrel.submitee.SubmiteeServer;

import java.util.Date;
import java.util.UUID;

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

    public static AttributeSerializer<Long> LONG = new AttributeSerializer<Long>() {
        @Override
        public Long parse(JsonElement json) {
            return json.getAsLong();
        }

        @Override
        public JsonElement write(Long aLong) {
            return new JsonPrimitive(aLong);
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

    public static AttributeSerializer<UUID> UUID = new AttributeSerializer<UUID>() {
        @Override
        public UUID parse(JsonElement json) {
            return java.util.UUID.fromString(json.getAsString());
        }

        @Override
        public JsonElement write(UUID uuid) {
            return SubmiteeServer.GSON.toJsonTree(uuid);
        }
    };
}
