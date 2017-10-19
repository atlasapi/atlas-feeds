package org.atlasapi.feeds.tasks.youview.processing;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

import com.google.common.collect.ImmutableSet;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Ignore
public class UpdateTaskTest {

    private static final Set<Status> NON_UPLOADED_STATUSES = ImmutableSet.of(
            Status.NEW,
            Status.PENDING
    );
    @Mock private FeedsTelescopeReporter telescope;
    
    private Task task = mock(Task.class);
    private TaskStore taskStore = mock(TaskStore.class);
    private TaskProcessor processor = mock(TaskProcessor.class);
    
    private final UpdateTask updateTask = new UpdateTask(taskStore, processor, null, YOUVIEW);
    
    @Test
    public void testProcessesAllNonFinalTasks() {
        when(task.action()).thenReturn(Action.UPDATE);
        
        for (Status status : NON_UPLOADED_STATUSES) {
            when(taskStore.allTasks(YOUVIEW, status)).thenReturn(ImmutableSet.of(task));
        }
        
        updateTask.run();
        
        verify(processor, times(NON_UPLOADED_STATUSES.size())).process(task, telescope);
    }
}
