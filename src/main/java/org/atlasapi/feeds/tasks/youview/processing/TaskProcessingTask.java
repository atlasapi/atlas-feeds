package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.TaskQuery;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporterFactory;

import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;
import com.metabroadcast.common.query.Selection;
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
    private final TelescopeReporterName reporterName;
    private final Publisher publisher;

    private static final List<TVAElementType> ELEMENT_TYPE_ORDER =
            Collections.unmodifiableList(Arrays.asList(
                    //do brands, series, then everything else (null).
                    TVAElementType.CHANNEL, TVAElementType.BRAND, TVAElementType.SERIES, TVAElementType.ITEM, null
            ));
    private static final int NUM_TO_CHECK_PER_ITTERATION = 1000;

    public TaskProcessingTask(
            TaskStore taskStore,
            TaskProcessor processor,
            Publisher publisher,
            DestinationType destinationType,
            TelescopeReporterName reporterName) {

        this.taskStore = checkNotNull(taskStore);
        this.processor = checkNotNull(processor);
        this.publisher = publisher;
        this.destinationType = checkNotNull(destinationType);
        this.reporterName = reporterName;
    }

    @Override
    protected void runTask() {
        UpdateProgress progress = UpdateProgress.START;
        FeedsTelescopeReporter telescope = FeedsTelescopeReporterFactory.getInstance()
                .getTelescopeReporter(reporterName);
        telescope.startReporting();

        //go through items based on type, then status, then in chunks of NUM_TO_CHECK_PER_ITTERATION
        for (TVAElementType elementType : ELEMENT_TYPE_ORDER) {
            for (Status status : validStatuses()) {
                //We limit the amount of stuff because too many cause a mongo driver exception
                //(presumably due to the sort on date)
                int numChecked = 0;
                do {
                    log.info("{} {} {} from publisher {} (batch {} to {})",
                            action(), status,
                            (elementType == null ? "" : elementType),
                            (publisher == null ? "ALL" : publisher),
                            numChecked, numChecked + NUM_TO_CHECK_PER_ITTERATION);

                    TaskQuery.Builder query = TaskQuery
                            .builder(
                                    Selection.limitedTo(NUM_TO_CHECK_PER_ITTERATION),
                                    destinationType
                            )
                            .withTaskStatus(status)
                            .withTaskAction(action())
                            // it is important that tasks are picked up in this order
                            // for example if a version A is moved from item B to item C, the
                            // revocation of A will be done first, and the recreation of A under C
                            // second. Picking up tasks in the wrong order will result in the
                            // permanent revocation of A.
                            .withSort(TaskQuery.Sort.DATE_ASC);

                    if (publisher != null) {
                        query.withPublisher(publisher);
                    }
                    if (elementType != null) {
                        query.withTaskType(elementType);
                    }

                    numChecked += processTasks(taskStore.allTasks(query.build()), progress, telescope);

                } while (numChecked > 0 && (numChecked % NUM_TO_CHECK_PER_ITTERATION) == 0);
                log.info("{} {} {} from publisher {} is finished. Total items processed {} ",
                        action(), status,
                        (elementType == null ? "" : elementType),
                        (publisher == null ? "ALL" : publisher),
                        numChecked);
            }
        }

        telescope.endReporting();
    }

    private int processTasks(Iterable<Task> tasksToCheck, UpdateProgress progress, FeedsTelescopeReporter telescope) {
        int tasksProcessed = 0;
        for (Task task : tasksToCheck) {
            if (!shouldContinue()) {
                break;
            }
            tasksProcessed++;
            try {
                processor.process(task, telescope);
                progress = progress.reduce(UpdateProgress.SUCCESS);
            } catch (Exception e) {
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
        return tasksProcessed;
    }
    public Publisher getPublisher(){
        return this.publisher;
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
