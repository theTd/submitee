package org.starrel.submitee.blob;

import org.starrel.submitee.ExceptionReporting;
import org.starrel.submitee.SubmiteeServer;
import org.starrel.submitee.attribute.AttributeFilter;
import org.starrel.submitee.attribute.AttributeMap;
import org.starrel.submitee.attribute.AttributeSpec;

import java.io.*;
import java.util.Date;
import java.util.UUID;

public class FileTreeBlobStorage implements BlobStorage {
    public final static String TYPE_ID = "file-tree";
    public final static String ATTRIBUTE_COLLECTION_NAME = "file-tree-blob-storages";

    private final String id;
    private final AttributeMap<FileTreeBlobStorage> attributeMap;
    private final AttributeSpec<String> uriSpec;
    private File directory;

    public FileTreeBlobStorage(String id) {
        this.id = id;

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
            ExceptionReporting.report("initializing blob storage " + getAttributePersistKey(), e);
        }
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Blob create(String fileName) throws IOException {
        if (directory == null) {
            throw new IOException("blob storage directory not set yet");
        }
        UUID uniqueId = UUID.randomUUID();
        String key = uniqueId.toString().replace("-", "");
        return new FileTreeBlob(fileName, key);
    }

    @Override
    public Blob get(String key) {
        return null;
    }

    @Override
    public String getAttributePersistKey() {
        return getId();
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
        private final String fileName;
        private final String key;
        private final Date createTime;
        private final File file;

        private FileTreeBlob(String fileName, String key) throws IOException {
            this.fileName = fileName;
            this.key = key;
            this.createTime = new Date();
            this.file = new File(directory + File.separator + key.substring(0, 2) + key);
            if (!file.getParentFile().mkdirs() || !this.file.createNewFile()) {
                throw new IOException("failed to create file");
            }
            //noinspection ResultOfMethodCallIgnored
            this.file.setExecutable(false);
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
