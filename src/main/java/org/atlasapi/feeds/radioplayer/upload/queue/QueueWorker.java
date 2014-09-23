package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;


public abstract class QueueWorker<T extends QueueTask> implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TaskQueue<T> uploadQueue;
    
    public QueueWorker(TaskQueue<T> uploadQueue) {
        this.uploadQueue = checkNotNull(uploadQueue);
    }

    @Override
    public void run() {
        while(true) {
            try {
                Optional<T> task = uploadQueue.fetchOne();
                if (task.isPresent()) {
                    processTask(task.get());
                } else {
                    // urgh
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                log.error("Error processing task {}", e); 
            }
        }
    }

    abstract void processTask(T task);
}
