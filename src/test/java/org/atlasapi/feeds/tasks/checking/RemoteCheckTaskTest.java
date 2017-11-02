package org.atlasapi.feeds.tasks.checking;

import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.TaskProcessor;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import org.mockito.Matchers;

public class RemoteCheckTaskTest {

    private static final Set<Status> NON_TERMINAL_STATUSES = ImmutableSet.of(
            Status.ACCEPTED,
            Status.VALIDATING,
            Status.QUARANTINED,
            Status.COMMITTING,
            Status.COMMITTED,
            Status.PUBLISHING
    );
    
    private Task task = mock(Task.class);
    private TaskStore taskStore = mock(TaskStore.class);
    private TaskProcessor processor = mock(TaskProcessor.class);
    
    private final RemoteCheckTask remoteCheckTask = new RemoteCheckTask(taskStore, processor, YOUVIEW);

    @Test
    public void testProcessesAllNonFinalTasks() {
        for (Status status : NON_TERMINAL_STATUSES) {
            when(taskStore.allTasks(Matchers.any())).thenReturn(ImmutableSet.of(task));
        }
        
        remoteCheckTask.run();
        
        verify(processor, times(NON_TERMINAL_STATUSES.size())).checkRemoteStatusOf(task);
    }

}
