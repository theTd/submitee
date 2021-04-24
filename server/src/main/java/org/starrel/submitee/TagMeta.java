package org.starrel.submitee;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.starrel.submitee.attribute.AttributeSerializer;

@Data
@AllArgsConstructor
public class TagMeta {
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String color;

    public final static AttributeSerializer<TagMeta> SERIALIZER = new AttributeSerializer<TagMeta>() {
        @Override
        public TagMeta parse(JsonElement json) {
            return new TagMeta(JsonUtil.parseString(json, "id"),
                    JsonUtil.parseString(json, "name"),
                    JsonUtil.parseString(json, "color"));
        }

        @Override
        public JsonElement write(TagMeta tagMeta) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", tagMeta.id);
            obj.addProperty("name", tagMeta.name);
            obj.addProperty("color", tagMeta.color);
            return obj;
        }
    };
}
