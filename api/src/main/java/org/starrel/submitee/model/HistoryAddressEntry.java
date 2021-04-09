package org.starrel.submitee.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.starrel.submitee.attribute.AttributeSerializer;

import java.util.Date;

@Data
@AllArgsConstructor
@Builder
public class HistoryAddressEntry {
    @Getter
    Date time;
    @Getter
    String address;

    public final static Serializer SERIALIZER = new Serializer();

    public static class Serializer extends AttributeSerializer<HistoryAddressEntry> {
        @Override
        public HistoryAddressEntry parse(JsonElement json) {
            JsonObject object = json.getAsJsonObject();
            return new HistoryAddressEntry(new Date(object.get("time").getAsLong()), object.get("address").getAsString());

        }

        @Override
        public JsonElement write(HistoryAddressEntry historyAddressEntry) {
            JsonObject object = new JsonObject();
            object.addProperty("time", historyAddressEntry.getTime().getTime());
            object.addProperty("address", historyAddressEntry.getAddress());
            return object;
        }
    }
}
