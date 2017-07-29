package org.atlasapi.feeds.tasks.youview.processing;

import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.reporting.telescope.TelescopeProxy;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import org.mockito.Mock;

@Ignore
public class DeleteTaskTest {

    private static final Set<Status> NON_UPLOADED_STATUSES = ImmutableSet.of(
            Status.NEW,
            Status.PENDING
    );

    @Mock TelescopeProxy telescope;

    private Task task = mock(Task.class);
    private TaskStore taskStore = mock(TaskStore.class);
    private TaskProcessor processor = mock(TaskProcessor.class);
    
    private final DeleteTask deleteTask = new DeleteTask(taskStore, processor, YOUVIEW);
    
    @Test
    public void testProcessesAllNonFinalTasks() {
        when(task.action()).thenReturn(Action.DELETE);
        
        for (Status status : NON_UPLOADED_STATUSES) {
            when(taskStore.allTasks(YOUVIEW, status)).thenReturn(ImmutableSet.of(task));
        }
        
        deleteTask.run();
        
        verify(processor, times(NON_UPLOADED_STATUSES.size())).process(task, telescope);
    }
}
