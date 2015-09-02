package org.atlasapi.feeds.tasks.maintainance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class TaskTrimmingTaskTest {

    private TaskStore taskStore = mock(TaskStore.class);
    private Clock clock = new TimeMachine();
    private Duration taskTrimmingWindow = Duration.standardDays(1);
    
    private final TaskTrimmingTask task = new TaskTrimmingTask(taskStore, clock, taskTrimmingWindow);
    
    @Test
    public void testTrimsCorrectTaskWindow() {
        DateTime removalDate = clock.now().minus(taskTrimmingWindow);
        
        task.run();
        
        verify(taskStore).removeBefore(removalDate);
    }

}
