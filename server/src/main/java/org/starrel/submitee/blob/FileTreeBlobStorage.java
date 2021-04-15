package org.starrel.submitee.blob;

import org.starrel.submitee.ClassifiedException;
import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;
import org.starrel.submitee.model.UserDescriptor;

import java.io.*;
import java.util.Date;

public class FileTreeBlobStorage implements BlobStorage {
    public final static String TYPE_ID = "file_tree";
    public final static BlobStorageProvider PROVIDER = new BlobStorageProvider() {
        @Override
        public String getTypeId() {
            return TYPE_ID;
        }

        @Override
        public BlobStorage createNewStorage(String name) {
            FileTreeBlobStorage s = new FileTreeBlobStorage(name);
            s.path.set("");
            s.setAttribute("provider", TYPE_ID);
            s.setAttribute("name", name);
            return s;
        }

        @Override
        public BlobStorage accessStorage(String name) {
            FileTreeBlobStorage s = new FileTreeBlobStorage(name);
            try {
                s.setupDirectory(s.path.get());
            } catch (ClassifiedException e) {
                ExceptionReporting.report(FileTreeBlobStorage.class, "setting up blob storage",
                        "setting up blob storage " + name, e);
            }
            return s;
        }
    };
    public final static String ATTRIBUTE_COLLECTION_NAME = "file-tree-blob-storages";

    private final String name;
    private final AttributeMap<FileTreeBlobStorage> attributeMap;
    private final AttributeSpec<String> path;
    private File directory;

    public FileTreeBlobStorage(String name) {
        this.name = name;

        this.attributeMap = SubmiteeServer.getInstance().readAttributeMap(this, ATTRIBUTE_COLLECTION_NAME);
        this.path = this.attributeMap.of("config.path", String.class);
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Blob create(int blobId, String key, String fileName, String contentType, UserDescriptor uploader) throws IOException {
        if (directory == null) {
            throw new IOException("blob storage directory not set yet");
        }
        return new FileTreeBlob(blobId, fileName, key, contentType, uploader);
    }

    @Override
    public Blob access(int blobId, String key, String fileName, Date createTime, String contentType, UserDescriptor uploader) throws IOException {
        FileTreeBlob blob = new FileTreeBlob(blobId, fileName, key, createTime, contentType, uploader);
        if (!(blob.file.exists() && blob.file.isFile())) {
            throw new IOException("target file missing");
        }
        return blob;
    }

    @Override
    public void validateConfiguration() throws ClassifiedException {
        if (path.get() == null || path.get().isEmpty()) {
            throw new ClassifiedException("EMPTY_DIRECTORY");
        }
        if (this.directory == null || !this.directory.toString().equals(path.get())) {
            setupDirectory(path.get());
        }
    }

    public String getPath() {
        return path.get();
    }

    @Override
    public String getAttributePersistKey() {
        return getName();
    }

    @Override
    public AttributeMap<? extends BlobStorage> getAttributeMap() {
        return attributeMap;
    }

    @Override
    public void attributeUpdated(String path) {
        if (AttributeMap.includePath(path, "config.path")) {
            try {
                setupDirectory(this.path.get());
            } catch (Exception e) {
                ExceptionReporting.report(FileTreeBlobStorage.class, "initializing directory", e);
            }
        }
    }

    private void setupDirectory(String path) throws ClassifiedException {
        if (path == null || path.isEmpty()) throw new ClassifiedException("EMPTY_PATH");

        File dir = new File(path);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                if (dir.canWrite()) {
                    this.directory = dir;
                } else {
                    throw new ClassifiedException("DIRECTORY_NOT_WRITEABLE");
                }
            } else {
                throw new ClassifiedException("TARGET_NOT_DIRECTORY");
            }
        } else {
            if (dir.mkdirs()) {
                this.directory = dir;
            } else {
                throw new ClassifiedException("CANNOT_CREATE_DIRECTORY");
            }
        }
    }

    private class FileTreeBlob implements Blob {
        private final int blobId;
        private final String fileName;
        private final String key;
        private final Date createTime;
        private final String contentType;
        private final UserDescriptor uploader;
        private final File file;
        private long size = -1;
        private boolean finishedUploading = false;

        private FileTreeBlob(int blobId, String fileName, String key, String contentType, UserDescriptor uploader) throws IOException {
            this(blobId, fileName, key, new Date(), contentType, uploader);
            if (!file.getParentFile().mkdirs() || !this.file.createNewFile()) {
                throw new IOException("failed to create file");
            }
            //noinspection ResultOfMethodCallIgnored
            this.file.setExecutable(false);
        }

        private FileTreeBlob(int blobId, String fileName, String key, Date createTime, String contentType, UserDescriptor uploader) {
            this.blobId = blobId;
            this.fileName = fileName;
            this.key = key;
            this.createTime = createTime;
            this.contentType = contentType;
            this.uploader = uploader;
            this.file = new File(directory + File.separator + key.substring(0, 2) + File.separator + key);
        }

        @Override
        public int getBlobId() {
            return blobId;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getFilename() {
            return fileName;
        }

        @Override
        public Date getCreateTime() {
            return createTime;
        }

        @Override
        public OutputStream getOutputStream() throws FileNotFoundException {
            return new FileOutputStream(file) {
                @Override
                public void close() throws IOException {
                    super.close();
                    finishedUploading = true;
                    size = file.length();
                }
            };
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public long getSize() {
            if (size == -1) {
                size = file.length();
            }
            return size;
        }

        @Override
        public boolean getFinishedUploading() {
            return finishedUploading;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public UserDescriptor getUploader() {
            return uploader;
        }

        @Override
        public void delete() {
            if (!file.delete()) {
                SubmiteeServer.getInstance().getLogger().warn("failed to delete blob storage file: " + file);
            }
        }
    }
}
