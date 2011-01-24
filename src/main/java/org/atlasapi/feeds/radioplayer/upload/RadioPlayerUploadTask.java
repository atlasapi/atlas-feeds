package org.atlasapi.feeds.radioplayer.upload;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedType;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerUploadTask implements Runnable {

    private final KnownTypeQueryExecutor queryExecutor;
    private final Iterable<RadioPlayerService> services;

    private FTPUploadResultRecorder recorder;
    private final FTPCredentials credentials;
    private RadioPlayerXMLValidator validator;
    private int lookAhead = 7;
    private int lookBack = 2;
    private AdapterLog log;

    public RadioPlayerUploadTask(KnownTypeQueryExecutor queryExecutor, FTPCredentials credentials, Iterable<RadioPlayerService> services) {
        this.queryExecutor = queryExecutor;
        this.credentials = credentials;
        this.services = services;
    }

    @Override
    public void run() {
        log("RadioPlayerUploadTask started.", null, Severity.INFO);
        
        CompletionService<FTPUploadResult> uploadRunner = new ExecutorCompletionService<FTPUploadResult>(Executors.newFixedThreadPool(2));
        int submissions = 0;
        int successes = 0;
        
        List<FTPUploadResult> results = Lists.newArrayList();

        try {
            FTPClient client = connectAndLogin();
            
            int days = lookBack + lookAhead + 1;
    
            for(RadioPlayerService service : services) {
                DateTime day = new LocalDate().toInterval(DateTimeZones.UTC).getStart().minusDays(lookBack);
                for(int i = 0; i < days; i++, day = day.plusDays(1)) {
                    String filename = filename(service,day);
                    try {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        RadioPlayerFeedType.PI.compileFeedFor(day, service, queryExecutor, out);
                        byte[] bytes = out.toByteArray();
                        
                        submissions++;
                        FTPUpload delegate = new ValidatingFTPFileUpload(validator, filename, bytes, new FTPFileUpload(client, filename, bytes));
                        //delegate = new RemoteCheckingFTPFileUpload(client, filename, delegate);
                        uploadRunner.submit(new LoggingFTPUpload(log, delegate));
                        
                    } catch (Exception e) {
                        log("Exception uploading file " + filename, e);
                        results.add(DefaultFTPUploadResult.failedUpload(filename).withCause(e).withMessage(e.getMessage()));
                    }
                }
            }
            
            successes = submissions;
            for(int i = 0; i < submissions; i++) {
                try {
                    FTPUploadResult result = uploadRunner.take().get();
                    if(!FTPUploadResultType.SUCCESS.equals(result.type())){
                        successes--;
                    }
                    results.add(result);
                } catch(Exception e) {
                    log("Couldn't record FTP Upload result", e);
                }
            }
    
            if(recorder != null) {
                for(FTPUploadResult result : results) {
                    recorder.record(result);
                }
            }

            client.logout();
            client.disconnect();
            
        } catch (Exception e) {
            log("Exception running RadioPlayerUploadTask", e);
        }
        log("RadioPlayerUploadTask finished. " + successes + " files uploaded successfully", null, Severity.INFO);
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

    private String filename(RadioPlayerService service, DateTime day) {
        return String.format("%s_%s_PI.xml", day.toString("yyyyMMdd"), service.getRadioplayerId());
    }

    public RadioPlayerUploadTask withResultRecorder(FTPUploadResultRecorder recorder) {
        this.recorder = recorder;
        return this;
    }

    public RadioPlayerUploadTask withValidator(RadioPlayerXMLValidator validator) {
        this.validator = validator;
        return this;
    }

    public RadioPlayerUploadTask withLookAhead(int lookAhead) {
        this.lookAhead = lookAhead;
        return this;
    }

    public RadioPlayerUploadTask withLookBack(int lookBack) {
        this.lookBack = lookBack;
        return this;
    }
    
    public RadioPlayerUploadTask withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
    
}
