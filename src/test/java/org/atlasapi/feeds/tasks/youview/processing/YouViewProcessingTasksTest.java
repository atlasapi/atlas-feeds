package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.mockito.Mock;

import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class YouViewProcessingTasksTest {

    private static final Set<Status> NON_UPLOADED_STATUSES = ImmutableSet.of(
            Status.NEW,
            Status.PENDING
    );

    private FeedsTelescopeReporter telescope = mock(FeedsTelescopeReporter.class);
    
    private Task task = mock(Task.class);
    private TaskStore taskStore = mock(TaskStore.class);
    private TaskProcessor processor = mock(TaskProcessor.class);
    
    private final UpdateTask updateTask = new UpdateTask(taskStore, processor, YOUVIEW, telescope);
    private final DeleteTask deleteTask = new DeleteTask(taskStore, processor, YOUVIEW, telescope);

    @Test
    public void processesAllNonFinalUpdateTasks() {
        when(task.action()).thenReturn(Action.UPDATE);

        for (Status status : NON_UPLOADED_STATUSES) {
            when(taskStore.allTasks(YOUVIEW, status)).thenReturn(ImmutableSet.of(task));
        }

        updateTask.run();

        verify(processor, times(NON_UPLOADED_STATUSES.size())).process(task, telescope);
    }

    @Test
    public void processesAllNonFinalDeleteTasks() {
        when(task.action()).thenReturn(Action.DELETE);

        for (Status status : NON_UPLOADED_STATUSES) {
            when(taskStore.allTasks(YOUVIEW, status)).thenReturn(ImmutableSet.of(task));
        }

        deleteTask.run();

        verify(processor, times(NON_UPLOADED_STATUSES.size())).process(task, telescope);
    }
}
