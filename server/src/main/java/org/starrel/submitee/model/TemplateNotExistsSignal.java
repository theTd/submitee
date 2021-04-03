package org.starrel.submitee.model;

public class TemplateNotExistsSignal extends Exception {
    public final static TemplateNotExistsSignal INSTANCE = new TemplateNotExistsSignal();

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
