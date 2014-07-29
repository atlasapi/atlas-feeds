package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.atlasapi.feeds.upload.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.time.Clock;


public class UploadQueueWorker extends QueueWorker<UploadTask> {
    
    public static final String ERROR_KEY = "error";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final UploadServicesSupplier uploaderSupplier;
    private final Clock clock;
    private final InteractionManager stateUpdater;
    private final FileCreator fileCreator;
    
    public UploadQueueWorker(TaskQueue<UploadTask> uploadQueue, UploadServicesSupplier uploaderSupplier, 
            Clock clock, FileCreator fileCreator, InteractionManager stateUpdater) {
        super(uploadQueue);
        this.uploaderSupplier = checkNotNull(uploaderSupplier);
        this.clock = checkNotNull(clock);
        this.fileCreator = checkNotNull(fileCreator);
        this.stateUpdater = checkNotNull(stateUpdater);
    }

    @Override
    public void processTask(UploadTask task) {
        try {
            UploadAttempt result = upload(task);
            recordResult(task, result);
        } catch (InvalidStateException e) {
            log.error("tried to process upload task for file without file record. upload task: {}", task);
        }
    }

    // N.B. this code assumes that any upload-specific timestamps are related to UPLOAD time, rather than
    // the time at which the task was enqueued.
    private UploadAttempt upload(UploadTask task) {
        try {
            final FileUpload file = fileCreator.createFile(task.service(), task.type(), task.date());
            Optional<FileUploader> uploader = uploaderSupplier.get(task.uploadService(), clock.now(), task.type());
            if (uploader.isPresent()) {
                try {
                    return uploader.get().upload(file);
                } catch (Exception e) {
                    log.error(String.format("Error on upload for remote service %s, task %s: {}", task.uploadService(), task), e);
                    return UploadAttempt.failedUpload(clock.now(), ImmutableMap.of(ERROR_KEY, String.valueOf(e)));
                }
            } else {
                log.error("No uploader found for remote service {}", task.uploadService());
                return UploadAttempt.failedUpload(clock.now(), ImmutableMap.of(ERROR_KEY, String.format("No uploader found for remote service %s", task.uploadService())));
            }

        } catch (IOException e) {
            log.error("Error on file creation: {}", e);
            return UploadAttempt.failedUpload(clock.now(), ImmutableMap.of(ERROR_KEY, String.valueOf(e)));
        }
    }

    private void recordResult(UploadTask task, UploadAttempt result) throws InvalidStateException {
        stateUpdater.recordUploadResult(task, result);
    }
}
