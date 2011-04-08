package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.failedUpload;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.successfulUpload;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.time.DateTimeZones;

public class CommonsFTPFileUploader implements FTPFileUploader {

    private static final int TIMEOUT = 15*1000; //15 seconds
    private static final Duration RECONNECT_DELAY = Duration.standardSeconds(5);
    private static final int UPLOAD_ATTEMPTS = 5;
    
    private static final FTPFileFilter ftpFilenameFilter = new FTPFileFilter() {
        @Override
        public boolean accept(FTPFile file) {
            return file.isFile() && file.getName().endsWith(".xml") && file.getName().startsWith("20");
        }
    };

    private final FTPCredentials credentials;

    public CommonsFTPFileUploader(FTPCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public FTPUploadResult upload(FTPUpload upload) throws Exception {
        return attemptUpload(upload);
    }
    
    public List<FileLastModified> listDir(String dir) {
        ImmutableList.Builder<FileLastModified> list = ImmutableList.builder();
        FTPClient client = tryToConnectAndLogin();
        
        if (client != null && client.isConnected()) {
            try {
                FTPFile[] files = client.listFiles(dir, ftpFilenameFilter);
                
                for (FTPFile file: files) {
                    list.add(new FileLastModified(file.getName(), new DateTime(file.getTimestamp(), DateTimeZones.UTC)));
                }
                
                return list.build();
            } catch (IOException e) {
                //TODO: remove
                e.printStackTrace();
            } finally {
                disconnectQuietly(client);
            }
        }
        
        return list.build();
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
            client.setConnectTimeout(TIMEOUT);

            client.connect(credentials.server(), credentials.port());

            client.setSoTimeout(TIMEOUT);

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

    public static class FileLastModified {
        
        private final String fileName;
        private final DateTime lastModified;

        public FileLastModified(String fileName, DateTime lastModified) {
            this.fileName = fileName;
            this.lastModified = lastModified;
        }
        
        public String fileName() {
            return fileName;
        }
        
        public DateTime lastModified() {
            return lastModified;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FileLastModified) {
                FileLastModified target = (FileLastModified) obj;
                return Objects.equal(fileName, target.fileName) && Objects.equal(lastModified, target.lastModified);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return fileName.hashCode();
        }
        
        @Override
        public String toString() {
            return Objects.toStringHelper(this).addValue(fileName).addValue(lastModified).toString();
        }
    }
}
