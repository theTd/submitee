package org.starrel.submitee.blob;

public class BlobNotFoundSignal extends Exception {
    public final static BlobNotFoundSignal INSTANCE = new BlobNotFoundSignal();

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
