package org.atlasapi.feeds.tasks.checking;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.atlasapi.BlockingExecutor;
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

    //We need this to limit this a surprisingly small number,
    // cause if there are too many tasks mongo sort overflows.
    private static final int NUM_TO_CHECK_PER_ITTERATION = 10;

    private final BlockingExecutor executor = new BlockingExecutor(50, 1000);

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
        final UpdateProgress[] progress = { UpdateProgress.START };
        
        //We will check tasks per status, and then in blocks of NUM_TO_CHECK_PER_ITERATION linked
        //through dates. This is because requesting all out once causes mongo overflow problems.
        for (Status status : TO_BE_CHECKED) {
            int numChecked = 0;
            boolean checkedInLastLoop;
            DateTime lastDateChecked = new DateTime().minusYears(1);
            do {
                log.info("Checking remote status for {}, already checked {}", status, numChecked);
                checkedInLastLoop = false;
                TaskQuery.Builder query = TaskQuery.builder(
                        Selection.limitedTo(NUM_TO_CHECK_PER_ITTERATION), destinationType)
                        .withTaskStatus(status)
                        .after(lastDateChecked)
                        .withSort(TaskQuery.Sort.of(
                                TaskQuery.Sort.Field.CREATED_TIME,
                                TaskQuery.Sort.Direction.ASC
                        ));

                Iterable<Task> tasksToCheck = taskStore.allTasks(query.build());

                for (Task task : tasksToCheck) {
                    if (!shouldContinue()) {
                        break;
                    }
                    try {
                        executor.execute(() -> {
                            processor.checkRemoteStatusOf(task);
                            progress[0] = progress[0].reduce(UpdateProgress.SUCCESS);
                        });
                    } catch (Exception e) {
                        log.error("error checking task {}", task.id(), e);
                        progress[0] = progress[0].reduce(UpdateProgress.FAILURE);
                    }
                    numChecked++;
                    checkedInLastLoop = true;
                    //if more than NUM_TO_CHECK_PER_ITTERATION exist with the same date, it is
                    //possible that this will get stuck in checking the same tasks over and over
                    //assuming they don't change state (e.g. stay QUARANTINED). We will thus
                    //increment that by a tiny bit to keep the loop from becoming stuck, and we
                    //will count on the next iteration to check things that might have been skipped.
                    lastDateChecked = task.created().plusMillis(1);
                    reportStatus(progress[0].toString());
                }
            } while (checkedInLastLoop);
            log.info("Done Checking remote status for {}. Checked {}", status, numChecked );
        }
    }



}
