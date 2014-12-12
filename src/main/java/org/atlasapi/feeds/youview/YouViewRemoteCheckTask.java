package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.feeds.youview.service.TaskProcessor;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;


public class YouViewRemoteCheckTask extends ScheduledTask {
    
    private static final Set<Status> TO_BE_CHECKED = ImmutableSet.of(
            Status.ACCEPTED,
            Status.VALIDATING,
            Status.QUARANTINED,
            Status.COMMITTING,
            Status.COMMITTED,
            Status.PUBLISHING
    );
    
    private final Logger log = LoggerFactory.getLogger(YouViewRemoteCheckTask.class);
    private final TaskStore taskStore;
    private final TaskProcessor processor;

    public YouViewRemoteCheckTask(TaskStore taskStore, TaskProcessor processor) {
        this.taskStore = checkNotNull(taskStore);
        this.processor = checkNotNull(processor);
    }

    @Override
    protected void runTask() {
        UpdateProgress progress = UpdateProgress.START;
        for (Status uncheckedStatus : TO_BE_CHECKED) {
            if (!shouldContinue()) {
                break;
            }
            Iterable<Task> tasksToCheck = taskStore.allTasks(uncheckedStatus);
            for (Task task : tasksToCheck) {
                if (!shouldContinue()) {
                    break;
                }
                try {
                    processor.checkRemoteStatusOf(task);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.error("error checking task {}", task.id(), e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                reportStatus(progress.toString());
            }
        }
    }
}
