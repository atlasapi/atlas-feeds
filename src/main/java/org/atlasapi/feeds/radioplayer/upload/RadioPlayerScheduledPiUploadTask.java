package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.LocalDate;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerScheduledPiUploadTask extends ScheduledTask {

    private final Iterable<RadioPlayerService> services;
    private final RadioPlayerRecordingExecutor executor;
    private final Iterable<FileUploadService> uploaders;
    private final DayRangeGenerator dayRangeGenerator;
    private final AdapterLog log;

    public RadioPlayerScheduledPiUploadTask(Iterable<FileUploadService> uploaders, RadioPlayerRecordingExecutor executor, Iterable<RadioPlayerService> services, DayRangeGenerator dayRangeGenerator, AdapterLog log) {
        this.uploaders = uploaders;
        this.executor = executor;
        this.services = services;
        this.dayRangeGenerator = dayRangeGenerator;
        this.log = log;
    }

    @Override
    public void runTask() {
        new RadioPlayerPiBatchUploadTask(uploaders, executor, services, dayRangeGenerator.generate(new LocalDate(DateTimeZones.UTC)), log).run();
        
    }

}
