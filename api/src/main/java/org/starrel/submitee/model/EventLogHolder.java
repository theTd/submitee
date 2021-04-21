package org.starrel.submitee.model;

import com.google.gson.JsonObject;

import java.util.Date;

public interface EventLogHolder {

    void pushEvent(String issuer, String eventType, JsonObject body);

    void pushEvent(String issuer, String eventType, JsonObject body, Date time);

}
