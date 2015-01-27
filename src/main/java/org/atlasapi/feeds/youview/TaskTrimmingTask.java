package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.joda.time.Duration;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.Clock;


public class TaskTrimmingTask extends ScheduledTask {
    
    private final TaskStore taskStore;
    private final Clock clock;
    private final Duration taskTrimmingWindow;
    
    public TaskTrimmingTask(TaskStore taskStore, Clock clock, Duration taskTrimmingWindow) {
        this.taskStore = checkNotNull(taskStore);
        this.clock = checkNotNull(clock);
        this.taskTrimmingWindow = checkNotNull(taskTrimmingWindow);
    }

    @Override
    protected void runTask() {
        taskStore.removeBefore(clock.now().minus(taskTrimmingWindow));
    }

}
