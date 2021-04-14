package org.starrel.submitee.model;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.starrel.submitee.attribute.AttributeSerializer;

import java.util.Date;

@Builder
@Data
public class UserDescriptor {
    @Getter
    String realmType;
    @Getter
    String userId;

    public UserDescriptor(String realmType, String userId) {
        Preconditions.checkNotNull(realmType);
        Preconditions.checkNotNull(userId);
        if (realmType.contains(":")) throw new IllegalArgumentException("realm type should not contains :");
        this.realmType = realmType;
        this.userId = userId;
    }

    public String toString() {
        return realmType + ":" + userId;
    }

    public static UserDescriptor parse(String userDescriptor) {
        int idx;
        if ((idx = userDescriptor.indexOf(":")) == -1) {
            throw new IllegalArgumentException("invalid user descriptor: " + userDescriptor);
        }
        String realm = userDescriptor.substring(0, idx);
        String userId = userDescriptor.substring(idx + 1);
        if (realm.isEmpty() || userId.isEmpty()) {
            throw new IllegalArgumentException("invalid user descriptor: " + userDescriptor);
        }

        return UserDescriptor.builder().realmType(realm).userId(userId).build();
    }

    public final static Serializer SERIALIZER = new Serializer();

    public static class Serializer extends AttributeSerializer<UserDescriptor> {
        @Override
        public UserDescriptor parse(JsonElement json) {
            JsonObject object = json.getAsJsonObject();
            return new UserDescriptor(object.get("realm-type").getAsString(), object.get("user-id").getAsString());

        }

        @Override
        public JsonElement write(UserDescriptor userDescriptor) {
            JsonObject object = new JsonObject();
            object.addProperty("realm-type", userDescriptor.realmType);
            object.addProperty("user-id", userDescriptor.userId);
            return object;
        }
    }
}
