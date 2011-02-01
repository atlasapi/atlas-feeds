package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.SUCCESSFUL;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.INFO;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerUploadTask implements Runnable {

    private final RadioPlayerUploadTaskRunner runner;
    private final Iterable<RadioPlayerService> services;
    private RadioPlayerXMLValidator validator;
    private FTPUploadResultRecorder recorder;
    private AdapterLog log;
    private int lookAhead = 0;
    private int lookBack = 0;
    private final KnownTypeQueryExecutor queryExecutor;

    public RadioPlayerUploadTask(RadioPlayerUploadTaskRunner runner, Iterable<RadioPlayerService> services, KnownTypeQueryExecutor queryExecutor) {
        this.runner = runner;
        this.services = services;
        this.queryExecutor = queryExecutor;
    }
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        int serviceCount = Iterables.size(services);
        int days = lookBack + lookAhead + 1;

        log(String.format("Radioplayer Uploader starting for %s services for %s days", serviceCount, days), INFO);
        
        List<FTPClient> clients = runner.getClients(10);
        int connections = clients.size();
        
        int submissions = 0;
        
        CompletionService<FTPUploadResult> resultRunner = new ExecutorCompletionService<FTPUploadResult>(runner.getExecutorService());
        
        for(RadioPlayerService service : services) {
            DateTime day = new LocalDate().toInterval(DateTimeZones.UTC).getStart().minusDays(lookBack);
            for(int i = 0; i < days; i++, day = day.plusDays(1)) {
                    FTPClient client = connections > 0 ? clients.get(submissions++ % connections) : null;
                    resultRunner.submit(new RadioPlayerFTPUploadTask(client, day, service, queryExecutor).withValidator(validator).withLog(log));
            }
        }
        
        int successes = 0;
        for (int i = 0; i < submissions; i++) {
            try {
                FTPUploadResult result = resultRunner.take().get();
                recorder.record(result);
                if(SUCCESSFUL.apply(result)) {
                    successes++;
                }
            } catch (InterruptedException e) {
                log("Radioplayer Uploader interrupted waiting for result.", WARN, e);
            } catch (ExecutionException e) {
                log("Radioplayer Uploader exception retrieving result", WARN, e);
            }
        }
        
        for (FTPClient ftpClient : clients) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                log("RadioPlayerUploader failed to disconnect FTP client", Severity.WARN, e);
            }
        }

        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        log(String.format("Radioplayer Uploader finished in %s, %s/%s successful.", runTime, successes, submissions), INFO);
    }
    
    private void log(String desc, Severity s) {
        log(desc, s, null);
    }
    
    private void log(String desc, Severity s, Exception e) {
        if(log != null) {
            AdapterLogEntry entry = new AdapterLogEntry(s).withDescription(desc).withSource(getClass());
            log.record(e != null ? entry.withCause(e) : entry);
        }
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
