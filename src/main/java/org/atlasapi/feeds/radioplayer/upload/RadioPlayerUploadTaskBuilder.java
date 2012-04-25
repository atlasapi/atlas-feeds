package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.LocalDate;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadTaskBuilder {

    private final Iterable<FileUploadService> uploadServices;
    private final RadioPlayerRecordingExecutor executor;
    private AdapterLog log;
    private final ContentLister contentLister;
    private final LastUpdatedContentFinder lastUpdatedContentFinder;

    public RadioPlayerUploadTaskBuilder(Iterable<FileUploadService> uploadServices, RadioPlayerRecordingExecutor executor, LastUpdatedContentFinder lastUpdatedContentFinder, ContentLister contentLister) {
        this.uploadServices = uploadServices;
        this.executor = executor;
        this.lastUpdatedContentFinder = lastUpdatedContentFinder;
        this.contentLister = contentLister;
    }
    
    public RadioPlayerUploadTaskBuilder withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
    
    public ScheduledTask newScheduledPiTask(Iterable<RadioPlayerService> services, DayRangeGenerator dayGenerator) {
        return new RadioPlayerScheduledPiUploadTask(uploadServices, executor, services, dayGenerator, log);
    }
    
    public Runnable newBatchPiTask(Iterable<RadioPlayerService> services, Iterable<LocalDate> days) {
        return new RadioPlayerPiBatchUploadTask(uploadServices, executor, services, days, log);
    }
    
    public ScheduledTask newScheduledOdTask(Iterable<RadioPlayerService> services, boolean fullSnapshot) {
        return new RadioPlayerScheduledOdUpdateTask(uploadServices, executor, services, log, fullSnapshot, lastUpdatedContentFinder, contentLister);
    }
    
}
