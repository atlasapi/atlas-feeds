package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.queue.InteractionManager;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;


public abstract class ScheduledUploadTask extends ScheduledTask {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Set<UploadService> uploadServices;
    private final DayRangeGenerator dayRangeGenerator;
    private final Set<RadioPlayerService> services;
    private final InteractionManager stateUpdater;
    
    public ScheduledUploadTask(Iterable<UploadService> uploadServices, DayRangeGenerator dayRangeGenerator, 
            Iterable<RadioPlayerService> services, InteractionManager stateUpdater) {
        this.uploadServices = ImmutableSet.copyOf(uploadServices);
        this.dayRangeGenerator = checkNotNull(dayRangeGenerator);
        this.services = ImmutableSet.copyOf(services);
        this.stateUpdater = checkNotNull(stateUpdater);
    }

    @Override
    protected void runTask() {
        UpdateProgress progress = UpdateProgress.START;
        for (UploadService uploadService : uploadServices) {
            for (LocalDate day : dayRangeGenerator.generate(new LocalDate(DateTimeZones.UTC))) {
                for (RadioPlayerService service : services) {
                    if (!shouldContinue()) {
                        return;
                    }
                    UploadTask task = new UploadTask(new RadioPlayerFile(uploadService, service, fileType(), day));
                    try {
                        stateUpdater.enqueueUploadTask(task);
                        progress = progress.reduce(UpdateProgress.SUCCESS);
                    } catch (Exception e) {
                        log.error("Error enqueueing task for upload", task, e);
                        progress = progress.reduce(UpdateProgress.FAILURE);
                        reportStatus(progress.toString());
                    }
                }
            }
        }
    }

    abstract FileType fileType();
}
