package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerOdBatchUploadTask implements Runnable {
    
    private final Iterable<FileUploadService> uploaders;
    private final RadioPlayerRecordingExecutor executor;
    private final Iterable<RadioPlayerService> services;
    private final AdapterLog log;
    private final boolean fullSnapshot;
    private final LocalDate day;
    private final Optional<DateTime> since;
    private final RadioPlayerOdUriResolver uriResolver;

    public RadioPlayerOdBatchUploadTask(Iterable<FileUploadService> uploaders, RadioPlayerRecordingExecutor executor, Iterable<RadioPlayerService> services, LocalDate day, boolean fullSnapshot, AdapterLog log, LastUpdatedContentFinder lastUpdatedContentFinder, ContentLister contentLister) {
        this.uploaders = uploaders;
        this.executor = executor;
        this.services = services;
        this.day = day;
        this.fullSnapshot = fullSnapshot;
        this.log = log;
        this.since = fullSnapshot ? Optional.<DateTime>absent() : Optional.of(day.toDateTimeAtStartOfDay(DateTimeZone.UTC).minusHours(2));
        this.uriResolver = new RadioPlayerOdUriResolver(contentLister, lastUpdatedContentFinder);
    }
    
    
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        int serviceCount = Iterables.size(services);
        log.record(AdapterLogEntry.infoEntry().withDescription("Radioplayer OD Batch Uploader starting for %s services", serviceCount));
        
        SetMultimap<RadioPlayerService,String> serviceToUris = fullSnapshot ? uriResolver.getServiceToUrisMapForSnapshot() : uriResolver.getServiceToUrisMapSince(since.get());
        
        log.record(AdapterLogEntry.infoEntry().withDescription("Radioplayer OD Batch Uploader finished finding uris to process"));
        
        List<Callable<Iterable<RadioPlayerUploadResult>>> uploadTasks = Lists.newArrayListWithCapacity(serviceCount);
        for (RadioPlayerService service : services) {
            uploadTasks.add(new RadioPlayerOdUploadTask(uploaders, since, day, service, serviceToUris.get(service), log));
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
                log.record(AdapterLogEntry.warnEntry().withDescription("Radioplayer OD Batch Uploader interrupted waiting for result.").withCause(e));
            } catch (ExecutionException e) {
                log.record(AdapterLogEntry.warnEntry().withDescription("Radioplayer OD Batch Uploader exception retrieving result").withCause(e));
            }
        }

        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        AdapterLogEntry.infoEntry().withDescription("Radioplayer OD Batch Uploader finished in %s, %s/%s successful.", runTime, successes, uploadTasks.size());
    }
}
