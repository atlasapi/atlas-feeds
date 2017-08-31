package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.reporting.telescope.AtlasFeedsReporters;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract base for classes to perform various actions upon a remote
 * system. It iterates through all {@link Task}s for a set of {@link Status}es,
 * and if the Task is for a particular {@link Action}, performs that action on 
 * the remote system 
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public abstract class TaskProcessingTask extends ScheduledTask {

    private final Logger log = LoggerFactory.getLogger(TaskProcessingTask.class);
    
    private final TaskStore taskStore;
    private final TaskProcessor processor;
    private final DestinationType destinationType;
    
    public TaskProcessingTask(TaskStore taskStore, TaskProcessor processor,
            DestinationType destinationType) {
        this.taskStore = checkNotNull(taskStore);
        this.processor = checkNotNull(processor);
        this.destinationType = checkNotNull(destinationType);
    }

    @Override
    protected void runTask() {
        UpdateProgress progress = UpdateProgress.START;
        FeedsTelescopeReporter telescope = FeedsTelescopeReporter.create(AtlasFeedsReporters.YOU_VIEW_ASYNC_UPLOADER);
        telescope.startReporting();

        for (Status uncheckedStatus : validStatuses()) {
            Iterable<Task> tasksToCheck = taskStore.allTasks(destinationType, uncheckedStatus);
            for (Task task : tasksToCheck) {
                if (!shouldContinue()) {
                    break;
                }
                if (!action().equals(task.action())) {
                    continue;
                }
                try {
                    log.info("task processor for atlasid={}", task.atlasDbId());
                    processor.process(task, telescope);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch(Exception e) {
                    log.error("Failed to process task {}", task, e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                    //report to telescope
                    String payload = task.payload().isPresent()
                                     ? task.payload().get().payload()
                                     : "";
                    telescope.reportFailedEvent(
                            "Failed to process taskId=" + task.id()
                            + ". destination " + task.destination()
                            + ". atlasId=" + task.atlasDbId()
                            + ". payload present=" + task.payload().isPresent()
                            + " (" + e.toString() + ")",
                            payload
                    );
                }
                reportStatus(progress.toString());
            }
        }

        telescope.endReporting();
    }

    /**
     * @return the {@Action} that this task is trying to process {@link Task}s for
     */
    public abstract Action action();

    /**
     * Returns the set of {@link Status}es representing {@link Task}s
     * that have not yet been processed fully i.e. they have neither been 
     * successfully processed nor failed terminally.  
     */
    public abstract Set<Status> validStatuses();
}
