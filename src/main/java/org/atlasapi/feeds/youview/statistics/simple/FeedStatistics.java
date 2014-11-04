package org.atlasapi.feeds.youview.statistics.simple;

import java.util.Date;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;

import com.google.common.base.Objects;


public class FeedStatistics {

    private Publisher publisher;
    private int queueSize;
    private Duration updateLatency;
    private Date lastOutage;
    
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
    }
    
    public Date lastOutage() {
        return lastOutage;
    }
    
    public void setLastOutage(Date lastOutage) {
        this.lastOutage = lastOutage;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(FeedStatistics.class)
                .add("publisher", publisher)
                .add("queueSize", queueSize)
                .add("updateLatency", updateLatency)
                .add("lastOutage", lastOutage)
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
