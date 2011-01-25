package org.atlasapi.feeds.radioplayer.upload;

import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;

public class FTPFileUpload implements FTPUpload {

    public FTPFileUpload() {
    }

    @Override
    public FTPUploadResult upload(FTPClient client, String filename, byte[] fileData) {
        try{
            synchronized (client) {
                OutputStream stream = client.storeFileStream(filename);
                stream.write(fileData);
                stream.close();
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
