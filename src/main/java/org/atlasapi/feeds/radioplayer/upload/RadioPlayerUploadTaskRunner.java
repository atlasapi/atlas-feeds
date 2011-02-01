package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Predicates.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioPlayerUploadTaskRunner {

    private static final int THREADS = 5;

    private final FTPCredentials credentials;
    private final CompletionService<FTPUploadResult> uploadRunner;
    private final ExecutorService executor;

    public RadioPlayerUploadTaskRunner(FTPCredentials credentials) {
        this.credentials = credentials;
        this.executor = Executors.newFixedThreadPool(THREADS, new ThreadFactoryBuilder().setNameFormat("RadioPlayerUploader: %s").build());
        this.uploadRunner = new ExecutorCompletionService<FTPUploadResult>(executor);
    }
    
    public Future<FTPUploadResult> submit(Callable<FTPUploadResult> uploadTask) {
        return uploadRunner.submit(uploadTask);
    }

    public List<FTPClient> getClients(int count) {
        ArrayList<FTPClient> clientList = Lists.newArrayListWithCapacity(count);
        for (int i = 0; i < count; i++) {
            clientList.add(connectAndLogin());
        }
        return ImmutableList.copyOf(Iterables.filter(clientList, notNull()));
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
            return null;
        }

    }
}
