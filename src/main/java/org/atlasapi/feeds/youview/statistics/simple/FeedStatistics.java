package org.atlasapi.feeds.youview.statistics.simple;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormat;

import com.google.common.base.Objects;


public class FeedStatistics {

    private Publisher publisher;
    private int queueSize;
    private Duration updateLatency;
    private String updateLatencyString;
    private int successfulTasks;
    private int unsuccessfulTasks;
    
    public FeedStatistics() {
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public int queueSize() {
        return queueSize;
    }
    
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
    
    public Duration updateLatency() {
        return updateLatency;
    }
    
    public void setUpdateLatency(Duration updateLatency) {
        this.updateLatency = updateLatency;
        this.updateLatencyString = PeriodFormat.getDefault().print(updateLatency.toPeriod());
    }

    public String getUpdateLatencyString() {
        return updateLatencyString;
    }

    public int successfulTasks() {
        return successfulTasks;
    }

    public void setSuccessfulTasks(int successfulTasks) {
        this.successfulTasks = successfulTasks;
    }

    public int unsuccessfulTasks() {
        return unsuccessfulTasks;
    }

    public void setUnsuccessfulTasks(int unsuccessfulTasks) {
        this.unsuccessfulTasks = unsuccessfulTasks;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(FeedStatistics.class)
                .add("publisher", publisher)
                .add("queueSize", queueSize)
                .add("updateLatency", updateLatency)
                .add("successfulTasks", successfulTasks)
                .add("unsuccessfulTasks", unsuccessfulTasks)
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
