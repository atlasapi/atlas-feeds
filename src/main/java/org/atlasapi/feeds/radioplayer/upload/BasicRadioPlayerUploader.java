package org.atlasapi.feeds.radioplayer.upload;

import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;

public class BasicRadioPlayerUploader implements RadioPlayerUploader {

    private final FTPClient client;

    public BasicRadioPlayerUploader(FTPClient client) {
        this.client = client;
    }
    
    @Override
    public RadioPlayerUploadResult upload(String filename, byte[] fileData) {
        try {
            OutputStream stream = client.storeFileStream(filename);
            stream.write(fileData);
            stream.close();
            if(!client.completePendingCommand()) {
                throw new Exception("Failed to complete pending command");
            }
            return DefaultRadioPlayerUploadResult.successfulUpload(filename);
        } catch (Exception e) {
            return DefaultRadioPlayerUploadResult.failedUpload(filename).withMessage("Failed to upload file").withCause(e);
        }
    }

}
