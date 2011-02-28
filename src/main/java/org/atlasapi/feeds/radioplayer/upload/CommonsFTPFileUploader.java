package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.failedUpload;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.successfulUpload;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.joda.time.Duration;

public class CommonsFTPFileUploader implements FTPFileUploader {

    private static final Duration RECONNECT_DELAY = Duration.standardSeconds(5);
    private static final int UPLOAD_ATTEMPTS = 5;

    private final FTPCredentials credentials;

    public CommonsFTPFileUploader(FTPCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public FTPUploadResult upload(FTPUpload upload) throws Exception {
        return attemptUpload(upload);
    }
    
    private FTPUploadResult attemptUpload(FTPUpload upload) throws InterruptedException {
        FTPUploadResult uploadResult = doUpload(upload);

        for (int i = 0; i < UPLOAD_ATTEMPTS && !uploadResult.type().equals(FTPUploadResultType.SUCCESS); i++) {
            Thread.sleep(RECONNECT_DELAY.getMillis());
            uploadResult = doUpload(upload);
        }

        return uploadResult;
    }
    
    public FTPUploadResult doUpload(FTPUpload upload) {
        String filename = upload.getFilename();
        FTPClient client = tryToConnectAndLogin();
        if (client == null) {
            return failedUpload(filename).withMessage("Failed to connect/login to server").withConnectionSuccess(false);
        } else if (client.isConnected()) {
            try {
                OutputStream stream = client.storeFileStream(filename);
                if (stream == null) {
                    return failedUpload(filename).withMessage(String.format("Failed to open stream to server. FTP Response: %s", client.getReplyString()));
                } else {
                    stream.write(upload.getFileData());
                    stream.close();
                    if (!client.completePendingCommand()) {
                        return failedUpload(filename).withMessage(String.format("Failed to complete upload to server. FTP Response: %s", client.getReplyString()));
                    }
                }
            } catch (IOException e) {
                return failedUpload(filename).withMessage("Connection to server dropped: " + e.getMessage()).withCause(e);
            } finally {
                disconnectQuietly(client);
            }
            return successfulUpload(filename).withMessage("File uploaded successfully");
        } else {
            return failedUpload(filename).withMessage("No connection to server").withConnectionSuccess(false);
        }
    }

    private FTPClient tryToConnectAndLogin() {
        try {
            FTPClient client = new FTPClient();

            client.connect(credentials.server(), credentials.port());

            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                client.disconnect();
                return null;
            }

            client.enterLocalPassiveMode();

            if (!client.login(credentials.username(), credentials.password())) {
                client.disconnect();
                return null;
            }

            return client;
        } catch (IOException e) {
            return null;
        }
    }

    public void disconnectQuietly(FTPClient client) {
        try {
            client.disconnect();
        } catch (IOException e) {
            // ignore failure...
        }
    }

}
