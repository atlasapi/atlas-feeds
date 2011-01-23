package org.atlasapi.feeds.radioplayer.upload;

import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;

public class FTPFileUpload implements FTPUpload {

    private final String filename;
    private final byte[] fileData;
    private final FTPClient client;

    public FTPFileUpload(FTPClient client, String filename, byte[] fileData) {
        this.client = client;
        this.filename = filename;
        this.fileData = fileData;
    }

    @Override
    public FTPUploadResult call() throws Exception {
        try{
            synchronized (client) {
                OutputStream output = client.storeFileStream(filename);
                output.write(fileData);
                if(!client.completePendingCommand()) {
                    throw new Exception("Couldn't complete file upload");
                }
            }
            return DefaultFTPUploadResult.successfulUpload(filename);
        } catch (Exception e) {
            return DefaultFTPUploadResult.failedUpload(filename).withMessage(e.getMessage()).withCause(e);
        }
    }

}
