package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.SUCCESSFUL;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.INFO;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
import com.google.common.collect.Maps;
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
        
        Map<String, Future<FTPUploadResult>> results = Maps.newHashMapWithExpectedSize(serviceCount * days);
        
        List<FTPClient> clients = runner.getClients(10);
        int connections = clients.size();
        int submissions = 0;
        
        for(RadioPlayerService service : services) {
            DateTime day = new LocalDate().toInterval(DateTimeZones.UTC).getStart().minusDays(lookBack);
            for(int i = 0; i < days; i++, day = day.plusDays(1)) {
                    FTPClient client = connections > 0 ? clients.get(submissions++ % connections) : null;
                    results.put(filename(service, day), runner.submit(new RadioPlayerFTPUploadTask(client, day, service, queryExecutor).withValidator(validator).withLog(log)));
            }
        }
        
        int successes = 0;
        for (Entry<String,Future<FTPUploadResult>> futureEntry : results.entrySet()) {
            FTPUploadResult result = null;
            try {
                result = futureEntry.getValue().get();
                recorder.record(result);
                if(SUCCESSFUL.apply(result)) {
                    successes++;
                }
            } catch (InterruptedException e) {
                log("Radioplayer Uploader interrupted waiting for result.", WARN);
            } catch (ExecutionException e) {
                result = DefaultFTPUploadResult.failedUpload(futureEntry.getKey()).withCause(e).withMessage("Error running uploader");
            }
            if(result != null) {
                recorder.record(result);
            }
        }

        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        log(String.format("Radioplayer Uploader finished in %s, %s/%s successful.", runTime, successes, submissions), INFO);
    }
    
    private void log(String desc, Severity s) {
        if(log != null) {
            log.record(new AdapterLogEntry(s).withDescription(desc).withSource(getClass()));
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

    private String filename(RadioPlayerService service, DateTime day) {
        return String.format("%s_%s_PI.xml", day.toString("yyyyMMdd"), service.getRadioplayerId());
    }
    
}
