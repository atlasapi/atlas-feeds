package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;

import com.google.common.base.Objects;


public class FeedStatistics {

    private final Publisher publisher;
    private final int queueSize;
    private final Duration updateLatency;
    
    public FeedStatistics(Publisher publisher, int queueSize, 
            Duration updateLatency) {
        this.publisher = checkNotNull(publisher);
        this.queueSize = queueSize;
        this.updateLatency = checkNotNull(updateLatency);
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
    
    @Override
    public String toString() {
        return Objects.toStringHelper(FeedStatistics.class)
                .add("publisher", publisher)
                .add("queueSize", queueSize)
                .add("updateLatency", updateLatency)
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
