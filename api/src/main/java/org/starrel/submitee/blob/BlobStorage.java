package org.starrel.submitee.blob;

public interface BlobStorage {

    String getTypeId();

    String getId();

    Blob create(String fileName);

    Blob get(String key);
}
