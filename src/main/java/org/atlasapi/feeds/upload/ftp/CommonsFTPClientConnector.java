package org.atlasapi.feeds.upload.ftp;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.atlasapi.feeds.upload.RemoteServiceDetails;

import com.metabroadcast.common.security.UsernameAndPassword;

public class CommonsFTPClientConnector {

    private static final int FIFTEEN_SECONDS = 15*1000;

    public FTPClient connectAndLogin(RemoteServiceDetails remoteDetails) throws ConnectException {

        try {
            FTPClient client = new FTPClient();
            client.setConnectTimeout(FIFTEEN_SECONDS);

            client.connect(remoteDetails.server().toString(), remoteDetails.port());

            client.setSoTimeout(FIFTEEN_SECONDS);

            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                client.disconnect();
                throw new IllegalStateException(String.format("Didn't connect successfully: %s %s ", client.getReplyCode(), client.getReplyString()));
            }

            client.enterLocalPassiveMode();

            if (remoteDetails.hasCredentials()) {
                UsernameAndPassword credentials = remoteDetails.credentials();
                if (!client.login(credentials.username(), credentials.password())) {
                    client.disconnect();
                    throw new IllegalStateException(String.format("Client didn't login successfully with username %s", credentials.username()));
                }
            }

            return client;
        } catch (Exception e) {
            throw new ConnectException(e.getMessage());
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
