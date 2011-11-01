package org.atlasapi.feeds.upload;

public class FileUpload {

    private final String filename;
    private final byte[] fileData;

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

}
