package org.atlasapi.feeds.radioplayer.upload;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedType;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerUploadTask implements Runnable {

    private final KnownTypeQueryExecutor queryExecutor;
    private final FTPUploadService ftpService;
    private final Iterable<RadioPlayerService> services;

    private FTPUploadResultRecorder recorder;
    private RadioPlayerXMLValidator validator;
    private int lookAhead = 7;
    private int lookBack = 2;
    private AdapterLog log;

    public RadioPlayerUploadTask(KnownTypeQueryExecutor queryExecutor, FTPUploadService ftpService, Iterable<RadioPlayerService> services) {
        this.queryExecutor = queryExecutor;
        this.ftpService = ftpService;
        this.services = services;
    }

    @Override
    public void run() {
        CompletionService<FTPUploadResult> uploadRunner = new ExecutorCompletionService<FTPUploadResult>(Executors.newFixedThreadPool(2));
        int submissions = 0;

        try {
            FTPClient client = ftpService.connect();
            
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
                        uploadRunner.submit(new RemoteCheckingFTPFileUpload(client, filename, new ValidatingFTPFileUpload(validator, filename, bytes, new FTPFileUpload(client, filename, bytes))));
                        
                    } catch (Exception e) {
                        log("Exception uploading file " + filename, e);
                    }
                }
            }
    
            if(recorder != null) {
                for(int i = 0; i < submissions; i++) {
                    recorder.record(uploadRunner.take().get());
                }
            }

        } catch (Exception e) {
            log("Exception running RadioPlayerUploadTask", e);
        }
        log("RadioPlayerUploadTask finished. " + submissions + " files uploaded", null);
    }

    private void log(String desc, Exception e) {
        if(log != null) {
            log.record(new AdapterLogEntry(Severity.ERROR).withCause(e).withDescription(desc).withSource(getClass()));
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

//    private Future<FTPUploadResult> futureFor(String filename, Exception e) {
//        final FTPUploadResult result = DefaultFTPUploadResult.failedUpload(filename).withMessage(e.getMessage()).withCause(e);
//        return new Future<FTPUploadResult>(){
//
//            private boolean cancelled = false;
//
//            @Override
//            public boolean cancel(boolean mayInterruptIfRunning) {
//                cancelled  = true;
//                return false;
//            }
//
//            @Override
//            public FTPUploadResult get() throws InterruptedException, ExecutionException {
//                return result;
//            }
//
//            @Override
//            public FTPUploadResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//                return result;
//            }
//
//            @Override
//            public boolean isCancelled() {
//                return cancelled;
//            }
//
//            @Override
//            public boolean isDone() {
//                return true;
//            }
//
//        };
//    }
}
