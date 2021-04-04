package org.starrel.submitee.model;

public class NotExistsSignal extends Exception {
    public final static NotExistsSignal INSTANCE = new NotExistsSignal();

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
