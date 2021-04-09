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
        System.out.println("SubmiteeFileItem.getInputStream");
        return blob.getInputStream();
    }

    @Override
    public String getContentType() {
        System.out.println("SubmiteeFileItem.getContentType");
        return blob.getContentType();
    }

    @Override
    public String getName() {
        System.out.println("SubmiteeFileItem.getName");
        return blob.getFilename();
    }

    @Override
    public boolean isInMemory() {
        System.out.println("SubmiteeFileItem.isInMemory");
        return false;
    }

    @Override
    public long getSize() {
        System.out.println("SubmiteeFileItem.getSize");
        return blob.getSize();
    }

    @Override
    public byte[] get() {
        System.out.println("SubmiteeFileItem.get");
        return null;
    }

    @Override
    public String getString(String s) throws UnsupportedEncodingException {
        System.out.println("SubmiteeFileItem.getString");
        return null;
    }

    @Override
    public String getString() {
        System.out.println("SubmiteeFileItem.getString");
        return null;
    }

    @Override
    public void write(File file) throws Exception {
        System.out.println("SubmiteeFileItem.write");
    }

    @Override
    public void delete() {
        System.out.println("SubmiteeFileItem.delete");
    }

    @Override
    public String getFieldName() {
        System.out.println("SubmiteeFileItem.getFieldName");
        return null;
    }

    @Override
    public void setFieldName(String s) {
        System.out.println("SubmiteeFileItem.setFieldName");
    }

    @Override
    public boolean isFormField() {
        System.out.println("SubmiteeFileItem.isFormField");
        return false;
    }

    @Override
    public void setFormField(boolean b) {
        System.out.println("SubmiteeFileItem.setFormField");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        System.out.println("SubmiteeFileItem.getOutputStream");
        return blob.getOutputStream();
    }

    @Override
    public FileItemHeaders getHeaders() {
        System.out.println("SubmiteeFileItem.getHeaders");
        return null;
    }

    @Override
    public void setHeaders(FileItemHeaders fileItemHeaders) {
        System.out.println("SubmiteeFileItem.setHeaders");
    }
}
