package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.LocalDate;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadTaskBuilder {

    private final Iterable<FileUploadService> uploadServices;
    private final RadioPlayerRecordingExecutor executor;
    private AdapterLog log;

    public RadioPlayerUploadTaskBuilder(Iterable<FileUploadService> uploadServices, RadioPlayerRecordingExecutor executor) {
        this.uploadServices = uploadServices;
        this.executor = executor;
    }
    
    public RadioPlayerUploadTaskBuilder withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
    
    public ScheduledTask newTask(Iterable<RadioPlayerService> services, DayRangeGenerator dayGenerator) {
        return new RadioPlayerScheduledUploadTask(uploadServices, executor, services, dayGenerator, log);
    }
    
    public Runnable newTask(Iterable<RadioPlayerService> services, Iterable<LocalDate> days) {
        return new RadioPlayerBatchUploadTask(uploadServices, executor, services, days, log);
    }
    
}
