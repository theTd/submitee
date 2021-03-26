package org.starrel.submitee.blob;

import org.starrel.submitee.attribute.AttributeHolder;

import java.io.IOException;

public interface BlobStorage extends AttributeHolder<BlobStorage> {

    String getTypeId();

    String getId();

    Blob create(String fileName) throws IOException;

    Blob get(String key);
}
