package org.starrel.submitee.blob;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;

import java.io.*;

public class SubmiteeFileItem implements FileItem {
    private final Blob blob;

    public SubmiteeFileItem(Blob blob) {
        this.blob = blob;
    }

    public Blob getBlob() {
        return blob;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return blob.getInputStream();
    }

    @Override
    public String getContentType() {
        return blob.getContentType();
    }

    @Override
    public String getName() {
        return blob.getFilename();
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public long getSize() {
        return blob.getSize();
    }

    @Override
    public byte[] get() {
        return null;
    }

    @Override
    public String getString(String s) throws UnsupportedEncodingException {
        return null;
    }

    @Override
    public String getString() {
        return null;
    }

    @Override
    public void write(File file) throws Exception {
    }

    @Override
    public void delete() {
    }

    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public void setFieldName(String s) {
    }

    @Override
    public boolean isFormField() {
        return false;
    }

    @Override
    public void setFormField(boolean b) {
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return blob.getOutputStream();
    }

    @Override
    public FileItemHeaders getHeaders() {
        return null;
    }

    @Override
    public void setHeaders(FileItemHeaders fileItemHeaders) {
    }
}
