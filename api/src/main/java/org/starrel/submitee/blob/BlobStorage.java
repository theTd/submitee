package org.starrel.submitee.blob;

import org.starrel.submitee.attribute.AttributeHolder;
import org.starrel.submitee.model.UserDescriptor;

import java.io.IOException;
import java.util.Date;

public interface BlobStorage extends AttributeHolder<BlobStorage> {

    String getTypeId();

    String getName();

    Blob create(int blobId, String key, String fileName, String contentType, UserDescriptor uploader) throws IOException;

    Blob access(int blobId, String key, String fileName, Date createTime, String contentType, UserDescriptor uploader) throws IOException;
}
