package org.atlasapi.feeds.upload;

import com.metabroadcast.common.media.MimeType;

public class FileUpload {

    public static Builder fileUpload(String filename, byte[] fileData) {
        return new Builder(filename,fileData);
    }
    
    public static class Builder {
        
        private final String filename;
        private final byte[] fileData;
        private MimeType contentType;

        public Builder(String filename, byte[] fileData) {
            this.filename = filename;
            this.fileData = fileData;
        }
        
        public Builder withContentType(MimeType type) {
            this.contentType = type;
            return this;
        }
        
        public FileUpload build() {
            final FileUpload fileUpload = new FileUpload(filename, fileData);
            fileUpload.contentType = contentType;
            return fileUpload;
        }
    }
    
    private final String filename;
    private final byte[] fileData;
    private MimeType contentType;

    public FileUpload(String filename, byte[] fileData) {
        this.filename = filename;
        this.fileData = fileData;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public MimeType getContentType() {
        return contentType;
    }
}
