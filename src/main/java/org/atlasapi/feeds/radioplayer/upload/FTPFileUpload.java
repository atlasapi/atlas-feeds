package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.failedUpload;
import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.successfulUpload;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;

public class FTPFileUpload implements FTPUpload {

    public FTPFileUpload() {
    }

    @Override
    public FTPUploadResult upload(FTPClient client, String filename, byte[] fileData) {
        if (client != null && client.isConnected()) {
            synchronized (client) {
                    try {
                        OutputStream stream = client.storeFileStream(filename);
                        stream.write(fileData);
                        stream.close();
                        if (!client.completePendingCommand()) {
                            return failedUpload(filename).withMessage("Failed to complete upload to server");
                        }
                    } catch (IOException e) {
                        return failedUpload(filename).withMessage("Connection to server failed: " + e.getMessage()).withCause(e);
                    }
            }
            return successfulUpload(filename).withMessage("File uploaded successfully");
        } else {
            return failedUpload(filename).withMessage("No connection to server");
        }
    }

}
