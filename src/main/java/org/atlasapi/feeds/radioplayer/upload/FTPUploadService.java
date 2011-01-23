package org.atlasapi.feeds.radioplayer.upload;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.net.ftp.FTPClient;

public class FTPUploadService {

    private final FTPCredentials credentials;
    private final String ftpPath;
    private FTPClient client;
    private AtomicInteger clients;

    public FTPUploadService(FTPCredentials credentials, String ftpPath) {
        this.credentials = credentials;
        this.ftpPath = ftpPath;
        this.client = null;
        this.clients = new AtomicInteger(0);
    }

    public FTPClient connect() throws Exception {
        if(clients.compareAndSet(0, 1)) {
            client = connectAndLogin();
        } else {
            clients.incrementAndGet();
        }
        return client;
    }

    private FTPClient connectAndLogin() throws Exception{
        FTPClient client = new FTPClient();
        
        client.connect(credentials.server(), credentials.port());
        
        if(!client.login(credentials.username(), credentials.password())) {
            throw new Exception("Failed to login");
        }
        
        if(!client.changeWorkingDirectory(ftpPath)) {
            throw new Exception("Failed to move to directory " + ftpPath);
        }
        
        return client;
    }

    public void disconnect() throws IOException {
        if(clients.compareAndSet(1, 0)) {
            client.disconnect();
        } else {
            clients.decrementAndGet();
        }
    }

    // public Future<FTPUploadResult> submit(FTPFileUpload fileUpload) {
    // if(clients.compareAndSet(0, 1)) {
    // client = connect();
    // }
    // Future<FTPUploadResult> result =
    // completionService.submit(fileUpload.withClient(client));
    // return result;
    // }

}
