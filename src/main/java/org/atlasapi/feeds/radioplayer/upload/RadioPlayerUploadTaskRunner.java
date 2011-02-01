package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Predicates.notNull;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioPlayerUploadTaskRunner {

    private static final int THREADS = 5;

    private final FTPCredentials credentials;
    private final FTPUploadResultRecorder recorder;
    private final AdapterLog log;
    
    private final ExecutorService executor;

    public RadioPlayerUploadTaskRunner(FTPCredentials credentials, FTPUploadResultRecorder recorder, AdapterLog log) {
        this.credentials = credentials;
        this.recorder = recorder;
        this.log = log;
        this.executor = Executors.newFixedThreadPool(THREADS, new ThreadFactoryBuilder().setNameFormat("RadioPlayerUploader: %s").build());
    }
    
    public ExecutorService getExecutorService() {
        return executor;
    }

    public List<FTPClient> getClients(int count) {
        ArrayList<FTPClient> clientList = Lists.newArrayListWithCapacity(count);
        for (int i = 0; i < count; i++) {
            clientList.add(connectAndLogin());
        }
        ImmutableList<FTPClient> connections = ImmutableList.copyOf(Iterables.filter(clientList, notNull()));

        if(connections.isEmpty()) {
            recorder.record(DefaultFTPUploadResult.failedUpload(String.format("%s:%s",credentials.server(),credentials.port())).withMessage("Failed to connect/login to server"));
        } else {
            recorder.record(DefaultFTPUploadResult.successfulUpload(String.format("%s:%s",credentials.server(),credentials.port())).withMessage("Connected and logged-in successully"));
        }
        
        return connections;
    }

    private FTPClient connectAndLogin() {
        try {
            FTPClient client = new FTPClient();
            
            client.connect(credentials.server(), credentials.port());
            
            if(!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                client.disconnect();
                return null;
            }
            
            client.enterLocalPassiveMode();
            
            if (!client.login(credentials.username(), credentials.password())) {
                client.disconnect();
                return null;
            }
            
            return client;
        } catch (Exception e) {
            log.record(new AdapterLogEntry(WARN).withCause(e).withSource(getClass()).withDescription("RadioPlayerUploader failed to connect client"));
            return null;
        }
    }
}
