package org.starrel.submitee.attribute;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.model.UserDescriptor;

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

    public static AttributeSerializer<UserDescriptor> USER_DESCRIPTOR = new AttributeSerializer<UserDescriptor>() {
        @Override
        public UserDescriptor parse(JsonElement json) {
            JsonObject body = json.getAsJsonObject();
            return UserDescriptor.builder()
                    .realmType(body.get("realm-type").getAsString())
                    .userId(body.get("user-id").getAsString())
                    .build();
        }

        @Override
        public JsonElement write(UserDescriptor userDescriptor) {
            JsonObject object = new JsonObject();
            object.addProperty("realm-type", userDescriptor.getRealmType());
            object.addProperty("user-id", userDescriptor.getUserId());
            return object;
        }
    };
}
