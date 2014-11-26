package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.upload.YouViewService;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.scheduling.ScheduledTask;


public class YouViewRemoteCheckTask extends ScheduledTask {
    
    private static final Set<Status> UNCHECKED = ImmutableSet.of(
            Status.ACCEPTED,
            Status.VALIDATING,
            Status.QUARANTINED,
            Status.COMMITTING,
            Status.COMMITTED,
            Status.PUBLISHING
    );
    private final TaskStore taskStore;
    private final YouViewService client;

    public YouViewRemoteCheckTask(TaskStore taskStore, YouViewService client) {
        this.taskStore = checkNotNull(taskStore);
        this.client = checkNotNull(client);
    }

    @Override
    protected void runTask() {
        for (Status uncheckedStatus : UNCHECKED) {
            Iterable<Task> tasksToCheck = taskStore.allTasks(uncheckedStatus);
            for (Task task : tasksToCheck) {
                client.checkRemoteStatusOf(task);
            }
        }
    }
}
