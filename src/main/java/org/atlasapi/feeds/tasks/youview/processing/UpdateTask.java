package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.persistence.TaskStore;

import com.google.common.collect.ImmutableSet;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

import static org.atlasapi.reporting.telescope.FeedsReporterNames.YOU_VIEW_AUTOMATIC_UPLOADER;

public class UpdateTask extends TaskProcessingTask {
    
    public UpdateTask(
            TaskStore taskStore,
            TaskProcessor processor,
            DestinationType destinationType
    ) {
        super(taskStore, processor, destinationType, YOU_VIEW_AUTOMATIC_UPLOADER, null);
    }

    // For tests, allow use of own telescope
    public UpdateTask(
            TaskStore taskStore,
            TaskProcessor processor,
            DestinationType destinationType,
            FeedsTelescopeReporter telescope
    ) {
        super(taskStore, processor, destinationType, YOU_VIEW_AUTOMATIC_UPLOADER, telescope);
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
