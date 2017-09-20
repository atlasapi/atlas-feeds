package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.reporting.telescope.FeedsReporterNames;

import com.google.common.collect.ImmutableSet;


public class UpdateTask extends TaskProcessingTask {
    
    public UpdateTask(TaskStore taskStore, TaskProcessor processor, 
            DestinationType destinationType) {
        super(taskStore, processor, destinationType,
                FeedsReporterNames.YOU_VIEW_AUTOMATIC_UPLOADER
        );
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
