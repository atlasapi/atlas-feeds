package org.atlasapi.feeds.tasks.checking;

import java.util.Set;

import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.TaskQuery;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.TaskProcessor;

import com.metabroadcast.common.query.Selection;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;

import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;


public class RemoteCheckTask extends ScheduledTask {
    
    public static final Set<Status> TO_BE_CHECKED = ImmutableSet.of(
            Status.ACCEPTED,
            Status.VALIDATING,
            Status.QUARANTINED,
            Status.COMMITTING,
            Status.COMMITTED,
            Status.PUBLISHING
    );

    //We need this limit to a reasonable number, cause if there are too many tasks mongo sort overflows
    private static final int NUM_TO_CHECK_PER_ITTERATION = 5000;
    
    private final Logger log = LoggerFactory.getLogger(RemoteCheckTask.class);
    private final TaskStore taskStore;
    private final TaskProcessor processor;
    private final DestinationType destinationType;

    public RemoteCheckTask(TaskStore taskStore, TaskProcessor processor, DestinationType destinationType) {
        this.taskStore = checkNotNull(taskStore);
        this.processor = checkNotNull(processor);
        this.destinationType = checkNotNull(destinationType);
    }

    @Override
    protected void runTask() {
        UpdateProgress progress = UpdateProgress.START;

        //We will check tasks per status, and then in blocks of NUM_TO_CHECK_PER_ITERATION linked
        //through dates.
        for (Status status : TO_BE_CHECKED) {
            int numChecked = 0;
            DateTime lastDateChecked = new DateTime().minusYears(1);
            do {
                log.info("Checking remote status for {}, already checked {}", status, numChecked );
                TaskQuery.Builder query = TaskQuery.builder(
                        Selection.limitedTo(NUM_TO_CHECK_PER_ITTERATION),
                        destinationType)
                        .withTaskStatus(status)
                        .after(lastDateChecked)
                        .withSort(TaskQuery.Sort.of(TaskQuery.Sort.Field.CREATED_TIME, TaskQuery.Sort.Direction.ASC));

                Iterable<Task> tasksToCheck = taskStore.allTasks(query.build());

                for (Task task : tasksToCheck) {
                    if (!shouldContinue()) {
                        break;
                    }
                    numChecked++; //if this goes up to the iteration limit, request for more
                    lastDateChecked = task.created().minusSeconds(1); //after this date
                    try {
                        processor.checkRemoteStatusOf(task);
                        progress = progress.reduce(UpdateProgress.SUCCESS);
                    } catch (Exception e) {
                        log.error("error checking task {}", task.id(), e);
                        progress = progress.reduce(UpdateProgress.FAILURE);
                    }
                    reportStatus(progress.toString());
                }
            } while (numChecked > 0 && (numChecked % NUM_TO_CHECK_PER_ITTERATION) == 0 );
        }
    }
}
