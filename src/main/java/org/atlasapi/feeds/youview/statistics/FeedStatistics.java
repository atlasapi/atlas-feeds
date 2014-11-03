package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Objects;


public class FeedStatistics {

    private final Publisher publisher;
    private final int queueSize;
    private final Duration updateLatency;
    private final DateTime lastOutage;
    
    public FeedStatistics(Publisher publisher, int queueSize, 
            Duration updateLatency, DateTime lastOutage) {
        this.publisher = checkNotNull(publisher);
        this.queueSize = queueSize;
        this.updateLatency = checkNotNull(updateLatency);
        this.lastOutage = checkNotNull(lastOutage);
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
    
    public DateTime lastOutage() {
        return lastOutage;
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
