package org.atlasapi.feeds.radioplayer.upload;

public class FTPUpload {

    private final String filename;
    private final byte[] fileData;

    public FTPUpload(String filename, byte[] fileData) {
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
