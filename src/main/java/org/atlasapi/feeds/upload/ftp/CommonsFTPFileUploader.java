package org.atlasapi.feeds.upload.ftp;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.joda.time.Duration;

public class CommonsFTPFileUploader implements FileUploader {

    private static final Duration RECONNECT_DELAY = Duration.standardSeconds(5);
    private static final int UPLOAD_ATTEMPTS = 5;
    
    private final RemoteServiceDetails remoteDetails;
    private CommonsFTPClientConnector clientConnector;

    public CommonsFTPFileUploader(RemoteServiceDetails remoteDetails) {
        this.remoteDetails = remoteDetails;
        this.clientConnector = new CommonsFTPClientConnector();
    }
    
    @Override
    public void upload(FileUpload upload) throws Exception {
        attemptUpload(upload, UPLOAD_ATTEMPTS);
    }
    
    private void attemptUpload(FileUpload upload, int remainingAttempts) throws Exception {
        try {
            tryConnectAndUpload(upload);
        } catch (Exception e) {
            if(remainingAttempts == 0) {
                throw e;
            }
            Thread.sleep(RECONNECT_DELAY.getMillis());
            attemptUpload(upload, remainingAttempts--);
        }
    }
    
    public void tryConnectAndUpload(FileUpload upload) throws Exception {
        
        FTPClient client = clientConnector.connectAndLogin(remoteDetails);
        
        if (!client.isConnected()) {
            throw new IllegalStateException("No connection to server");
        } else {
            try {
                doUpload(client, upload);
            } finally {
                clientConnector.disconnectQuietly(client);
            }
        }
    }

    private void doUpload(FTPClient client, FileUpload upload) throws IOException {
        OutputStream stream = client.storeFileStream(upload.getFilename());
        if (stream == null) {
            throw new IllegalStateException(String.format("Failed to open file stream: %s %s", client.getReplyCode(), client.getReplyString()));
        }

        stream.write(upload.getFileData());
        stream.close();
        
        if (!client.completePendingCommand()) {
            throw new IllegalStateException(String.format("Failed to complete upload: %s %s", client.getReplyCode(), client.getReplyString()));
        }
    }
    
}
