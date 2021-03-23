package org.starrel.submitee.model;

import java.util.concurrent.CompletableFuture;

public interface NotificationScheme {

    String getTypeId();

    CompletableFuture<?> send(String literalAddress, String message);

}
