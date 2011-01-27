package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.failedUpload;
import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.successfulUpload;

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
            return successfulUpload(filename).withMessage("File uploaded successfully");
        } catch (Exception e) {
            return failedUpload(filename).withMessage(e.getMessage()).withCause(e);
        }
    }

}
