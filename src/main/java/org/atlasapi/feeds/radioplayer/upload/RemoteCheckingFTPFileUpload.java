package org.atlasapi.feeds.radioplayer.upload;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;

public class RemoteCheckingFTPFileUpload implements FTPUpload {

    private final FTPClient client;
    private final String filename;
    private final FTPUpload delegate;

    public RemoteCheckingFTPFileUpload(FTPClient client, String filename, FTPUpload delegate) {
        this.client = client;
        this.filename = filename;
        this.delegate = delegate;
    }

    @Override
    public FTPUploadResult call() throws Exception {
        FTPUploadResult delegateResult = delegate.call();
        if(FTPUploadResultType.SUCCESS.equals(delegateResult.type())) {
            return remoteCheck();
        }
        return delegateResult;
    }

    private FTPUploadResult remoteCheck() {
        try {
            for(int i = 0; i < 5; i++) {
                if(checkForFile("Processed")) {
                    return DefaultFTPUploadResult.successfulUpload(filename).withMessage("Success verified on remote host");
                }
                
                if(checkForFile("Failed")) {
                    return DefaultFTPUploadResult.failedUpload(filename).withMessage("Processing failed on remote host");
                }
                
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            return DefaultFTPUploadResult.unknownUpload(filename).withMessage("Couldn't verify success on remote host").withCause(e);
        }
        return DefaultFTPUploadResult.unknownUpload(filename).withMessage("Couldn't verify success on remote host");
    }
    
    private Boolean checkForFile(String pathname) throws IOException {
        FTPFile[] listFiles = null;
        synchronized (client) {
            client.changeWorkingDirectory(pathname);
            listFiles = client.listFiles(filename);
            client.changeToParentDirectory();
        }
        if(listFiles != null && listFiles.length == 1 && filename.equals(listFiles[0].getName())) {
            return true;
        }
        return false;
    }

}
