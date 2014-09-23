package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadManager;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
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
    private final UploadManager stateUpdater;
    private final FileHistoryStore fileStore;
    
    public ScheduledUploadTask(Iterable<UploadService> uploadServices, DayRangeGenerator dayRangeGenerator, 
            Iterable<RadioPlayerService> services, UploadManager stateUpdater, FileHistoryStore fileStore) {
        this.uploadServices = ImmutableSet.copyOf(uploadServices);
        this.dayRangeGenerator = checkNotNull(dayRangeGenerator);
        this.services = ImmutableSet.copyOf(services);
        this.stateUpdater = checkNotNull(stateUpdater);
        this.fileStore = checkNotNull(fileStore);
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
                    RadioPlayerFile file = new RadioPlayerFile(uploadService, service, fileType(), day);
                    UploadTask task = new UploadTask(file);
                    try {
                        createFileRecordIfNotPresent(file);
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

    private void createFileRecordIfNotPresent(RadioPlayerFile file) {
        Optional<FileHistory> existing = fileStore.fetch(file);
        if (!existing.isPresent()) {
            fileStore.store(new FileHistory(file));
        }
    }

    abstract FileType fileType();
}
