package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.failedUpload;
import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.successfulUpload;
import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.unknownUpload;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.SUCCESS;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class RemoteCheckingFTPFileUpload implements FTPUpload {

    private final FTPUpload delegate;

    public RemoteCheckingFTPFileUpload(FTPUpload delegate) {
        this.delegate = delegate;
    }

    @Override
    public FTPUploadResult upload(FTPClient client, String filename, byte[] fileData) {
        FTPUploadResult delegateResult = delegate.upload(client, filename, fileData);
        if(SUCCESS.equals(delegateResult.type())) {
            return remoteCheck(client, filename);
        }
        return delegateResult;
    }

    private FTPUploadResult remoteCheck(FTPClient client, String filename) {
        try {
            for(int i = 0; i < 5; i++) {
                if(checkForFile("Processed", client, filename)) {
                    return successfulUpload(filename).withMessage("Success verified on remote host");
                }
                
                if(checkForFile("Failed", client, filename)) {
                    return failedUpload(filename).withMessage("Processing failed on remote host");
                }
                
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            return unknownUpload(filename).withMessage("Couldn't verify success on remote host").withCause(e);
        }
        return unknownUpload(filename).withMessage("Couldn't verify success on remote host");
    }
    
    private Boolean checkForFile(String pathname, FTPClient client, String filename) throws IOException {
        FTPFile[] listFiles = null;
        synchronized (client) {
            if(client.changeWorkingDirectory(pathname)) {
                listFiles = client.listFiles(filename);
                client.changeToParentDirectory();
            }
        }
        if(listFiles != null && listFiles.length == 1 && filename.equals(listFiles[0].getName())) {
            return true;
        }
        return false;
    }

}
