package org.starrel.submitee.model;

import org.starrel.submitee.Util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class EmailNotificationScheme implements NotificationScheme {
    private final static String TYPE_ID = "email";

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public CompletableFuture<?> send(String literalAddress, String subject, String message, String abbrev) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Util.sendNotificationEmail(literalAddress, subject, message, abbrev).get();
            } catch (InterruptedException ignored) {
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }
}
