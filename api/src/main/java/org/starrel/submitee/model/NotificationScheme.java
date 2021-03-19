package org.starrel.submitee.model;

public interface NotificationScheme {

    String getTypeId();

    void send(String address, String message);

}
