package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerBatchUploadTask implements Runnable {

    private final Iterable<FileUploadService> uploaders;
    private final RadioPlayerRecordingExecutor executor;
    private final Iterable<RadioPlayerService> services;
    private final Iterable<LocalDate> days;
    private final AdapterLog log;

    public RadioPlayerBatchUploadTask(Iterable<FileUploadService> uploaders, RadioPlayerRecordingExecutor executor, Iterable<RadioPlayerService> services, Iterable<LocalDate> dayRange, AdapterLog log) {
        this.uploaders = uploaders;
        this.executor = executor;
        this.services = services;
        this.days = dayRange;
        this.log = log;
    }
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        int serviceCount = Iterables.size(services);
        log.record(AdapterLogEntry.infoEntry().withDescription("Radioplayer Uploader starting for %s services for %s days", serviceCount, Iterables.size(days)));

        List<Callable<Iterable<RadioPlayerUploadResult>>> uploadTasks = Lists.newArrayListWithCapacity(Iterables.size(days) * serviceCount);
        
        for (RadioPlayerService service : services) {
            for (LocalDate day : days) {
                uploadTasks.add(new RadioPlayerUploadTask(uploaders, day, service, log));
            }
        }

        LinkedBlockingQueue<Future<Iterable<RadioPlayerUploadResult>>> futureResults = executor.submit(uploadTasks);

        int successes = 0;
        for (int i = 0; i < uploadTasks.size(); i++) {
            try {
                Future<Iterable<RadioPlayerUploadResult>> futureResult = futureResults.take();
                if(!futureResult.isCancelled()) {
                    Iterable<RadioPlayerUploadResult> results = futureResult.get();
                    for (RadioPlayerUploadResult result : results) {
                        if (SUCCESS.equals(result.getUpload().type())) {
                            successes++;
                        }
                    }
                }
            }catch (InterruptedException e) {
                log.record(AdapterLogEntry.warnEntry().withDescription("Radioplayer Uploader interrupted waiting for result.").withCause(e));
            } catch (ExecutionException e) {
                log.record(AdapterLogEntry.warnEntry().withDescription("Radioplayer Uploader exception retrieving result").withCause(e));
            }
        }

        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        AdapterLogEntry.infoEntry().withDescription("Radioplayer Uploader finished in %s, %s/%s successful.", runTime, successes, uploadTasks.size());
    }

}
