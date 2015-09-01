package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.persistence.TaskStore;

import com.google.common.collect.ImmutableSet;


public final class UpdateTask extends TaskProcessingTask {
    
    public UpdateTask(TaskStore taskStore, TaskProcessor processor, 
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
