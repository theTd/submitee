package org.starrel.submitee.language;

public class MissingLanguageSignal extends RuntimeException {
    public final static MissingLanguageSignal INSTANCE = new MissingLanguageSignal();
    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
