package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.reporting.telescope.FeedsReporterNames;

import com.google.common.collect.ImmutableSet;


public final class DeleteTask extends TaskProcessingTask {
    
    public DeleteTask(
            TaskStore taskStore,
            TaskProcessor processor,
            Publisher publisher,
            DestinationType destinationType) {

        super(
                taskStore,
                processor,
                publisher,
                destinationType,
                FeedsReporterNames.YOU_VIEW_AUTOMATIC_DELETER
        );
    }

    private static final Set<Status> TO_BE_UPLOADED = ImmutableSet.of(
            Status.NEW,
            Status.PENDING
    );

    @Override
    public Action action() {
        return Action.DELETE;
    }

    @Override
    public Set<Status> validStatuses() {
        return TO_BE_UPLOADED;
    }

}
