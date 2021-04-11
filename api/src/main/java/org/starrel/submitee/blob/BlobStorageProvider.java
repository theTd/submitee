package org.starrel.submitee.blob;

import org.starrel.submitee.ClassifiedException;

public interface BlobStorageProvider {
    String getTypeId();

    BlobStorage createNewStorage(String name);

    BlobStorage accessStorage(String name) throws ClassifiedException;
}
