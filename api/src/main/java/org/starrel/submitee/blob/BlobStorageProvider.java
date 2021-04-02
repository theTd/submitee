package org.starrel.submitee.blob;

public interface BlobStorageProvider {
    String getTypeId();

    BlobStorage createNewStorage(String name);

    BlobStorage accessStorage(String name);
}
