package org.atlasapi.feeds.upload.ftp;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.FileUploaderResult;
import org.atlasapi.feeds.upload.RemoteServiceDetails;

public class CommonsFTPFileUploader implements FileUploader {

    private static final int UPLOAD_ATTEMPTS = 5;
    
    private final RemoteServiceDetails remoteDetails;
    private CommonsFTPClientConnector clientConnector;

    public CommonsFTPFileUploader(RemoteServiceDetails remoteDetails) {
        this.remoteDetails = remoteDetails;
        this.clientConnector = new CommonsFTPClientConnector();
    }
    
    @Override
    public FileUploaderResult upload(FileUpload upload) throws Exception {
        attemptUpload(upload);
        return FileUploaderResult.success();
    }
    
    private void attemptUpload(FileUpload upload) throws Exception {
        Exception exception = null;
        for (int i = 0; i < UPLOAD_ATTEMPTS; i++) {
            exception = tryConnectAndUpload(upload);
            if(exception == null) {
                return;
            }
        }
        throw exception;
    }
    
    public Exception tryConnectAndUpload(FileUpload upload) {
        try {
            FTPClient client = clientConnector.connectAndLogin(remoteDetails);
            
            if (!client.isConnected()) {
                return new IllegalStateException("No connection to server");
            } else {
                try {
                    doUpload(client, upload);
                } finally {
                    clientConnector.disconnectQuietly(client);
                }
            }
            return null;
        } catch (Exception e) {
            return e;
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
