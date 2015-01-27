package org.atlasapi.feeds.youview.tasks.upload;

import java.util.Set;

import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Destination.DestinationType;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.tasks.processing.TaskProcessor;

import com.google.common.collect.ImmutableSet;


public final class UploadTask extends TaskProcessingTask {
    
    public UploadTask(TaskStore taskStore, TaskProcessor processor, 
            DestinationType destinationType) {
        super(taskStore, processor, destinationType);
    }

    private static final Set<Status> TO_BE_UPLOADED = ImmutableSet.of(
            Status.NEW,
            Status.PENDING
    );
    private static final Action TO_UPLOAD = Action.UPDATE; 

    @Override
    public Action action() {
        return TO_UPLOAD;
    }

    @Override
    public Set<Status> validStatuses() {
        return TO_BE_UPLOADED;
    }

}
