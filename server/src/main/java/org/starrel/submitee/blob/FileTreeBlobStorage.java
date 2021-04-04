package org.starrel.submitee.blob;

import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeFilter;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;

import java.io.*;
import java.util.Date;

public class FileTreeBlobStorage implements BlobStorage {
    public final static BlobStorageProvider PROVIDER = new BlobStorageProvider() {
        @Override
        public String getTypeId() {
            return TYPE_ID;
        }

        @Override
        public BlobStorage createNewStorage(String name) {
            return new FileTreeBlobStorage(name);
        }

        @Override
        public BlobStorage accessStorage(String name) {
            return new FileTreeBlobStorage(name);
        }
    };

    public final static String TYPE_ID = "file-tree";
    public final static String ATTRIBUTE_COLLECTION_NAME = "file-tree-blob-storages";

    private final String name;
    private final AttributeMap<FileTreeBlobStorage> attributeMap;
    private final AttributeSpec<String> uriSpec;
    private File directory;

    public FileTreeBlobStorage(String name) {
        this.name = name;

        this.attributeMap = SubmiteeServer.getInstance().readAttributeMap(this, ATTRIBUTE_COLLECTION_NAME);
        this.uriSpec = this.attributeMap.of("uri", String.class);
        this.uriSpec.addFilter(new AttributeFilter<String>() {
            @Override
            public void onSet(String value) throws FilterException {
                try {
                    initializeDirectory(value);
                } catch (IOException e) {
                    throw new FilterException(e.getMessage());
                }
            }

            @Override
            public void onDelete(String path) throws FilterException {
                directory = null;
            }
        });

        try {
            initializeDirectory(uriSpec.get());
        } catch (IOException e) {
            ExceptionReporting.report(FileTreeBlobStorage.class, "initializing directory", e);
        }
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
    public Blob create(int blobId, String key, String fileName) throws IOException {
        if (directory == null) {
            throw new IOException("blob storage directory not set yet");
        }
        return new FileTreeBlob(blobId, fileName, key);
    }

    @Override
    public Blob access(int blobId, String key, String fileName, Date createTime) throws IOException {
        FileTreeBlob blob = new FileTreeBlob(blobId, fileName, key, createTime);
        if (!(blob.file.exists() && blob.file.isFile())) {
            throw new IOException("target file missing");
        }
        return blob;
    }

    @Override
    public String getAttributePersistKey() {
        return getName();
    }

    @Override
    public AttributeMap<? extends BlobStorage> getAttributeMap() {
        return attributeMap;
    }

    public String getUri() {
        return uriSpec.get();
    }

    @Override
    public void attributeUpdated(String path) {
    }

    private void initializeDirectory(String uri) throws IOException {
        File dir = new File(uri);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                if (dir.canWrite()) {
                    this.directory = dir;
                } else {
                    throw new IOException("cannot write to target directory");
                }
            } else {
                throw new IOException("target uri is not a directory");
            }
        } else {
            if (dir.mkdirs()) {
                this.directory = dir;
            } else {
                throw new IOException("failed to create directory");
            }
        }
    }

    private class FileTreeBlob implements Blob {
        private final int blobId;
        private final String fileName;
        private final String key;
        private final Date createTime;
        private final File file;

        private FileTreeBlob(int blobId, String fileName, String key) throws IOException {
            this(blobId, fileName, key, new Date());
            if (!file.getParentFile().mkdirs() || !this.file.createNewFile()) {
                throw new IOException("failed to create file");
            }
            //noinspection ResultOfMethodCallIgnored
            this.file.setExecutable(false);
        }

        private FileTreeBlob(int blobId, String fileName, String key, Date createTime) {
            this.blobId = blobId;
            this.fileName = fileName;
            this.key = key;
            this.createTime = createTime;
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
        public OutputStream getOutputStream() {
            try {
                return new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        @Override
        public InputStream getInputStream() {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        @Override
        public void delete() {
            if (!file.delete()) {
                SubmiteeServer.getInstance().getLogger().warn("failed to delete blob storage file: " + file);
            }
        }
    }
}
