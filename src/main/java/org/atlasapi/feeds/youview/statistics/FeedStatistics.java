package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;

import com.google.common.base.Objects;


public class FeedStatistics {

    private final Publisher publisher;
    private final int queueSize;
    private final Duration updateLatency;
    private final int createdTasks;
    private final int failedTasks;
    
    public FeedStatistics(
            Publisher publisher,
            int queueSize,
            Duration updateLatency,
            int createdTasks,
            int failedTasks
    ) {
        this.publisher = checkNotNull(publisher);
        this.queueSize = queueSize;
        this.updateLatency = checkNotNull(updateLatency);
        this.createdTasks = createdTasks;
        this.failedTasks = failedTasks;
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public int queueSize() {
        return queueSize;
    }
    
    public Duration updateLatency() {
        return updateLatency;
    }

    public int getCreatedTasks() {
        return createdTasks;
    }

    public int getFailedTasks() {
        return failedTasks;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(FeedStatistics.class)
                .add("publisher", publisher)
                .add("queueSize", queueSize)
                .add("updateLatency", updateLatency)
                .add("createdTasks", createdTasks)
                .add("failedTasks", failedTasks)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(publisher);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof FeedStatistics) {
            FeedStatistics other = (FeedStatistics) that;
            return publisher.equals(other.publisher);
        }
        
        return false;
    }
}
