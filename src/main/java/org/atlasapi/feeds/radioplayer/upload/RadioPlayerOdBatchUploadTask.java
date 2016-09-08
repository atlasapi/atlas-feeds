package org.atlasapi.feeds.radioplayer.upload;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.atlasapi.feeds.radioplayer.RadioPlayerOdFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;

import com.metabroadcast.common.time.DateTimeZones;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.NO_OP;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;

public class RadioPlayerOdBatchUploadTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RadioPlayerOdBatchUploadTask.class);
    
    private final Iterable<FileUploadService> uploaders;
    private final RadioPlayerRecordingExecutor executor;
    private final Iterable<RadioPlayerService> services;
    private final AdapterLog adapterLog;
    private final boolean fullSnapshot;
    private final LocalDate day;
    private final Optional<DateTime> since;
    private final RadioPlayerOdUriResolver uriResolver;
    private final Publisher publisher;
    private final RadioPlayerUploadResultStore resultStore;

    public RadioPlayerOdBatchUploadTask(
            Iterable<FileUploadService> uploaders,
            RadioPlayerRecordingExecutor executor,
            Iterable<RadioPlayerService> services,
            LocalDate day,
            boolean fullSnapshot,
            AdapterLog adapterLog,
            LastUpdatedContentFinder lastUpdatedContentFinder,
            ContentLister contentLister,
            Publisher publisher,
            RadioPlayerUploadResultStore resultStore
    ) {
        this.uploaders = uploaders;
        this.executor = executor;
        this.services = services;
        this.day = day;
        this.fullSnapshot = fullSnapshot;
        this.adapterLog = adapterLog;
        this.publisher = publisher;
        this.since = fullSnapshot
                     ? Optional.absent()
                     : Optional.of(day.toDateTimeAtStartOfDay(DateTimeZone.UTC).minusHours(2));
        this.uriResolver = new RadioPlayerOdUriResolver(
                contentLister, lastUpdatedContentFinder, publisher
        );
        this.resultStore = checkNotNull(resultStore);
    }
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        int serviceCount = Iterables.size(services);

        logInfo("Radioplayer OD Batch Uploader starting for %s services", serviceCount);

        SetMultimap<RadioPlayerService,String> serviceToUris =
                fullSnapshot
                ? uriResolver.getServiceToUrisMapForSnapshot()
                : uriResolver.getServiceToUrisMapSince(since.get());

        logInfo("Radioplayer OD Batch Uploader finished finding uris to process");

        List<Callable<Iterable<RadioPlayerUploadResult>>> uploadTasks =
                Lists.newArrayListWithCapacity(serviceCount);

        for (RadioPlayerService service : services) {
            Set<String> uris = serviceToUris.get(service);

            if (uris.isEmpty()) {
                logInfo("No items for OD %s upload for service %s",
                        (fullSnapshot ? "snapshot" : "change"), service);

                resultStore.record(
                        new RadioPlayerUploadResult(
                                FileType.OD,
                                service,
                                day,
                                new FileUploadResult(
                                        null,
                                        new RadioPlayerOdFeedSpec(
                                                service,
                                                day,
                                                since,
                                                ImmutableSet.of()
                                        ).filename(),
                                        DateTime.now(),
                                        NO_OP
                                )
                        )
                );
            } else {
                uploadTasks.add(new RadioPlayerOdUploadTask(uploaders, since, day, service, uris,
                        adapterLog, publisher));
            }
        }

        LinkedBlockingQueue<Future<Iterable<RadioPlayerUploadResult>>> futureResults =
                executor.submit(uploadTasks);

        int successes = 0;
        for (int i = 0; i < uploadTasks.size(); i++) {
            try {
                Future<Iterable<RadioPlayerUploadResult>> futureResult = futureResults.take();
                if(!futureResult.isCancelled()) {
                    Iterable<RadioPlayerUploadResult> results = futureResult.get();

                    boolean noResultsFound = true;
                    for (RadioPlayerUploadResult result : results) {
                        noResultsFound = false;
                        if (SUCCESS.equals(result.getUpload().type())) {
                            successes++;
                        }
                    }

                    if (noResultsFound) {
                        logWarn("Radioplayer OD Batch Uploader task returned no results");
                    }
                } else {
                    logWarn("Radioplayer OD Batch Uploader task interrupted");
                }
            } catch (InterruptedException e) {
                logWarn("Radioplayer OD Batch Uploader interrupted waiting for result.", e);
            } catch (ExecutionException e) {
                logWarn("Radioplayer OD Batch Uploader exception retrieving result", e);
            }
        }

        String runTime = new Period(start, new DateTime(DateTimeZones.UTC))
                .toString(PeriodFormat.getDefault());

        logInfo("Radioplayer OD Batch Uploader finished in %s, %s/%s successful.",
                runTime, successes, uploadTasks.size());
    }

    private void logInfo(String message, Object... args) {
        adapterLog.record(
                AdapterLogEntry.infoEntry()
                        .withDescription(message, args)
                        .withSource(getClass())
        );
        log.info(String.format(message, args));
    }

    private void logWarn(String message, Exception e, Object... args) {
        adapterLog.record(AdapterLogEntry.warnEntry()
                .withDescription(message, args)
                .withCause(e)
        );
        log.warn(String.format(message, args), e);
    }

    private void logWarn(String message, Object... args) {
        adapterLog.record(AdapterLogEntry.warnEntry()
                .withDescription(message, args)
        );
        log.warn(String.format(message, args));
    }
}
