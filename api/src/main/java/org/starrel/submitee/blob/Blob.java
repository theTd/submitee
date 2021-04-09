package org.starrel.submitee.blob;

import org.starrel.submitee.model.UserDescriptor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public interface Blob {
    int getBlobId();

    String getKey();

    String getFilename();

    Date getCreateTime();

    OutputStream getOutputStream() throws FileNotFoundException;

    InputStream getInputStream() throws IOException;

    long getSize();

    boolean getFinishedUploading();

    String getContentType();

    UserDescriptor getUploader();

    void delete();
}
