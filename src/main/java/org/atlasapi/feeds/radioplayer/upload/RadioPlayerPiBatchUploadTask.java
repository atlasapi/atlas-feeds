package org.atlasapi.feeds.radioplayer.upload;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.reporting.telescope.FeedsReporterNames;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporterFactory;

import com.metabroadcast.common.time.DateTimeZones;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;

public class RadioPlayerPiBatchUploadTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RadioPlayerPiBatchUploadTask.class);

    private final Iterable<FileUploadService> uploaders;
    private final RadioPlayerRecordingExecutor executor;
    private final Iterable<RadioPlayerService> services;
    private final Iterable<LocalDate> days;
    private final AdapterLog adapterLog;
    private final Publisher publisher;
    private FeedsReporterNames telescopeName;

    public RadioPlayerPiBatchUploadTask(
            Iterable<FileUploadService> uploaders,
            RadioPlayerRecordingExecutor executor,
            Iterable<RadioPlayerService> services,
            Iterable<LocalDate> dayRange,
            AdapterLog adapterLog,
            Publisher publisher,
            FeedsReporterNames telescopeName
    ) {
        this.uploaders = uploaders;
        this.executor = executor;
        this.services = services;
        this.days = dayRange;
        this.adapterLog = adapterLog;
        this.publisher = publisher;
        this.telescopeName = telescopeName;
    }
    
    @Override
    public void run() {
        FeedsTelescopeReporter telescopeReporter =
                FeedsTelescopeReporterFactory.getInstance().getTelescopeReporter(telescopeName);
        telescopeReporter.startReporting();

        DateTime start = new DateTime(DateTimeZones.UTC);
        int serviceCount = Iterables.size(services);

        logInfo("Radioplayer PI Batch Uploader starting for %s services for %s days",
                serviceCount, Iterables.size(days));

        List<Callable<Iterable<RadioPlayerUploadResult>>> uploadTasks =
                Lists.newArrayListWithCapacity(Iterables.size(days) * serviceCount);
        
        for (RadioPlayerService service : services) {
            for (LocalDate day : days) {
                uploadTasks.add(new RadioPlayerPiUploadTask(uploaders, day, service,
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
                        telescopeReporter.reportEvent(result);
                    }

                    if (noResultsFound) {
                        logWarn("Radioplayer PI Batch Uploader task returned no results");
                    }
                } else {
                    logWarn("Radioplayer PI Batch Uploader task interrupted");
                }
            } catch (InterruptedException e) {
                logWarn("Radioplayer PI Batch Uploader interrupted waiting for result.",e );
            } catch (ExecutionException e) {
                logWarn("Radioplayer PI Batch Uploader exception retrieving result", e);
            }
        }

        String runTime = new Period(start, new DateTime(DateTimeZones.UTC))
                .toString(PeriodFormat.getDefault());

        telescopeReporter.endReporting();
        logInfo("Radioplayer PI Batch Uploader finished in %s, %s/%s successful.",
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
