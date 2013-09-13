package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import com.metabroadcast.common.scheduling.ScheduledTask;

public class RadioPlayerScheduledOdUpdateTask extends ScheduledTask {
    
    private final RadioPlayerUploadServicesSupplier uploadersSupplier;
    private final RadioPlayerRecordingExecutor executor;
    private final Iterable<RadioPlayerService> services;
    private final AdapterLog log;
    private final boolean fullSnapshot;
    private final LastUpdatedContentFinder lastUpdatedContentFinder;
    private final ContentLister contentLister;
    private final Publisher publisher;

    public RadioPlayerScheduledOdUpdateTask(RadioPlayerUploadServicesSupplier uploadServicesSupplier, RadioPlayerRecordingExecutor executor, Iterable<RadioPlayerService> services, AdapterLog log, boolean fullSnapshot, LastUpdatedContentFinder lastUpdatedContentFinder, ContentLister contentLister, Publisher publisher) {
        this.uploadersSupplier = uploadServicesSupplier;
        this.executor = executor;
        this.services = services;
        this.log = log;
        this.fullSnapshot = fullSnapshot;
        this.lastUpdatedContentFinder = lastUpdatedContentFinder;
        this.contentLister = contentLister;
        this.publisher = publisher;
    }
    
    @Override
    protected void runTask() {
        LocalDate date = new LocalDate(DateTimeZone.UTC);
        if (fullSnapshot) {
            date = date.minusDays(1);
        }
        new RadioPlayerOdBatchUploadTask(uploadersSupplier.get(new DateTime(DateTimeZone.UTC), FileType.OD), executor, services, date, fullSnapshot, log, lastUpdatedContentFinder, contentLister, publisher).run();
    }
}
