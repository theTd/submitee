package org.starrel.submitee.blob;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public interface Blob {
    String getKey();

    String getFilename();

    Date getCreateTime();

    OutputStream getOutputStream();

    InputStream getInputStream();

    void delete();
}
