package org.starrel.submitee.attribute;

public class InvalidAttributeSignal extends Exception {
    public InvalidAttributeSignal(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
