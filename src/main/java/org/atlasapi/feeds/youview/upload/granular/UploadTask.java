package org.atlasapi.feeds.youview.upload.granular;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.feeds.youview.service.TaskProcessor;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.scheduling.ScheduledTask;


// TODO move to upload package
public class UploadTask extends ScheduledTask {

    private static final Set<Status> NOT_UPLOADED = ImmutableSet.of(
            Status.NEW
    );
    
    private final TaskStore taskStore;
    private final TaskProcessor processor;

    public UploadTask(TaskStore taskStore, TaskProcessor processor) {
        this.taskStore = checkNotNull(taskStore);
        this.processor = checkNotNull(processor);
    }

    @Override
    protected void runTask() {
        for (Status uncheckedStatus : NOT_UPLOADED) {
            Iterable<Task> tasksToCheck = taskStore.allTasks(uncheckedStatus);
            for (Task task : tasksToCheck) {
                processor.process(task);
            }
        }
    }
}
