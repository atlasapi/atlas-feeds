package org.atlasapi.feeds.radioplayer.upload;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerUploadTaskRunner implements Runnable {

    private static final int THREADS = 5;
    private final KnownTypeQueryExecutor queryExecutor;
    private final Iterable<RadioPlayerService> services;

    private FTPUploadResultRecorder recorder;
    private final FTPCredentials credentials;
    private RadioPlayerXMLValidator validator;
    private int lookAhead = 7;
    private int lookBack = 2;
    private AdapterLog log;

    public RadioPlayerUploadTaskRunner(KnownTypeQueryExecutor queryExecutor, FTPCredentials credentials, Iterable<RadioPlayerService> services) {
        this.queryExecutor = queryExecutor;
        this.credentials = credentials;
        this.services = services;
    }

    @Override
    public void run() {
        log("RadioPlayerUploadTask started.", null, Severity.INFO);
        
        CompletionService<FTPUploadResult> uploadRunner = new ExecutorCompletionService<FTPUploadResult>(Executors.newFixedThreadPool(THREADS));
        int submissions = 0;
        
        List<FTPUploadResult> results = Lists.newArrayList();

        try {
            FTPClient client = null;
            try{
                client = connectAndLogin();
                recorder.record(DefaultFTPUploadResult.successfulUpload(String.format("%s:%s",credentials.server(),credentials.port())).withMessage("Connected and logged-in successully"));
            } catch (Exception e) {
                recorder.record(DefaultFTPUploadResult.failedUpload(String.format("%s:%s",credentials.server(),credentials.port())).withMessage("Failed to connect/login to server").withCause(e));
            }
            
            int days = lookBack + lookAhead + 1;
    
            for(RadioPlayerService service : services) {
                DateTime day = new LocalDate().toInterval(DateTimeZones.UTC).getStart().minusDays(lookBack);
                for(int i = 0; i < days; i++, day = day.plusDays(1)) {
                        submissions++;
                        uploadRunner.submit(new RadioPlayerFTPUploadTask(client, day, service, queryExecutor).withValidator(validator).withLog(log));
                }
            }
            
            for(int i = 0; i < submissions; i++) {
                try {
                    results.add(uploadRunner.take().get());
                } catch(Exception e) {
                    log("Couldn't record FTP Upload result", e);
                }
            }
    
            if(recorder != null) {
                for(FTPUploadResult result : results) {
                    recorder.record(result);
                }
            }
            
            if(client != null) {
                client.logout();
                client.disconnect();
            }
            
        } catch (Exception e) {
            log("Exception running RadioPlayerUploadTask", e);
        }
        log("RadioPlayerUploadTask finished. " + successes(results) + " files uploaded successfully", null, Severity.INFO);
    }

    private int successes(List<FTPUploadResult> results) {
        return Iterables.size(Iterables.filter(results, DefaultFTPUploadResult.IS_SUCCESS));
    }

    private FTPClient connectAndLogin() throws Exception {
        FTPClient client = new FTPClient();

        client.connect(credentials.server(), credentials.port());
        
        client.enterLocalPassiveMode();
        
        if (!client.login(credentials.username(), credentials.password())) {
            throw new RuntimeException("Unable to connect to " + credentials.server() + " with username: " + credentials.username() + " and password...");
        }
        
        return client;
    }

    private void log(String desc, Exception e) {
        log(desc, e, Severity.ERROR);
    }
    
    private void log(String desc, Exception e, Severity s) {
        if(log != null) {
            AdapterLogEntry entry = new AdapterLogEntry(s).withDescription(desc).withSource(getClass());
            log.record(e == null ? entry : entry.withCause(e));
        }
    }
    
    public RadioPlayerUploadTaskRunner withResultRecorder(FTPUploadResultRecorder recorder) {
        this.recorder = recorder;
        return this;
    }

    public RadioPlayerUploadTaskRunner withValidator(RadioPlayerXMLValidator validator) {
        this.validator = validator;
        return this;
    }

    public RadioPlayerUploadTaskRunner withLookAhead(int lookAhead) {
        this.lookAhead = lookAhead;
        return this;
    }

    public RadioPlayerUploadTaskRunner withLookBack(int lookBack) {
        this.lookBack = lookBack;
        return this;
    }
    
    public RadioPlayerUploadTaskRunner withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
    
}
