package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;


public class YouViewRemoteCheckTask extends ScheduledTask {
    
    private static final Set<Status> UNCHECKED = ImmutableSet.of(
            Status.ACCEPTED,
            Status.VALIDATING,
            Status.QUARANTINED,
            Status.COMMITTING,
            Status.COMMITTED,
            Status.PUBLISHING
    );
    
    private final Logger log = LoggerFactory.getLogger(YouViewRemoteCheckTask.class);
    private final TaskStore taskStore;
    private final GranularYouViewService client;

    public YouViewRemoteCheckTask(TaskStore taskStore, GranularYouViewService client) {
        this.taskStore = checkNotNull(taskStore);
        this.client = checkNotNull(client);
    }

    @Override
    protected void runTask() {
        UpdateProgress progress = UpdateProgress.START;
        for (Status uncheckedStatus : UNCHECKED) {
            if (!shouldContinue()) {
                break;
            }
            Iterable<Task> tasksToCheck = taskStore.allTasks(uncheckedStatus);
            for (Task task : tasksToCheck) {
                if (!shouldContinue()) {
                    break;
                }
                try {
                    client.checkRemoteStatusOf(task);
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
