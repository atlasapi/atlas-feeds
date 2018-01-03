package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporterFactory;

import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;
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
    private final FeedsTelescopeReporter telescope;

    public TaskProcessingTask(
            TaskStore taskStore,
            TaskProcessor processor,
            DestinationType destinationType,
            FeedsTelescopeReporter telescope
    ) {
        this.taskStore = checkNotNull(taskStore);
        this.processor = checkNotNull(processor);
        this.destinationType = checkNotNull(destinationType);
        this.telescope = checkNotNull(telescope);
    }

    protected TaskProcessingTask(
            TaskStore taskStore,
            TaskProcessor processor,
            DestinationType destinationType,
            TelescopeReporterName reporterName
    ) {
        this.taskStore = checkNotNull(taskStore);
        this.processor = checkNotNull(processor);
        this.destinationType = checkNotNull(destinationType);
        this.telescope = FeedsTelescopeReporterFactory.getInstance()
                .getTelescopeReporter(reporterName);
    }

    @Override
    protected void runTask() {
        UpdateProgress progress = UpdateProgress.START;

        telescope.startReporting();

        for (Status uncheckedStatus : validStatuses()) {
            Iterable<Task> tasksToCheck = taskStore.allTasks(destinationType, uncheckedStatus);
            for (Task task : tasksToCheck) { //NOSONAR
                if (!shouldContinue()) {
                    break;
                }
                if (!action().equals(task.action())) {
                    continue;
                }
                try {
                    processor.process(task, telescope);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch(Exception e) {
                    log.error("Failed to process task {}", task, e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                    telescope.reportFailedEvent(
                            task,
                            "Failed to process taskId=" + task.id()
                            + ". destination " + task.destination()
                            + ". atlasId=" + task.atlasDbId()
                            + ". payload present=" + task.payload().isPresent()
                            + " (" + e.toString() + ")"
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
