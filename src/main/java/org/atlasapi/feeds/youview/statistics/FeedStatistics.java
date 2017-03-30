package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;

import com.google.common.base.Objects;


public class FeedStatistics {

    private final Publisher publisher;
    private final int queueSize;
    private final Duration updateLatency;
    private final int successfulTasks;
    private final int unsuccessfulTasks;
    
    private FeedStatistics(Builder builder) {
        this.publisher = checkNotNull(builder.publisher);
        this.queueSize = builder.queueSize;
        this.updateLatency = checkNotNull(builder.updateLatency);
        this.successfulTasks = builder.successfulTasks;
        this.unsuccessfulTasks = builder.unsuccessfulTasks;
    }

    public static Builder build() {
        return new Builder();
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

    public int successfulTasks() {
        return successfulTasks;
    }

    public int unsuccessfulTasks() {
        return unsuccessfulTasks;
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

    public static class Builder {

        private Publisher publisher;
        private int queueSize;
        private Duration updateLatency;
        private int successfulTasks;
        private int unsuccessfulTasks;

        private Builder() {}

        public Builder withPublisher(Publisher publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder withQueueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public Builder withUpdateLatency(Duration updateLatency) {
            this.updateLatency = updateLatency;
            return this;
        }

        public Builder withSuccessfulTasks(int successfulTasks) {
            this.successfulTasks = successfulTasks;
            return this;
        }

        public Builder withUnsuccessfulTasks(int unsuccessfulTasks) {
            this.unsuccessfulTasks = unsuccessfulTasks;
            return this;
        }

        public FeedStatistics build() {
            return new FeedStatistics(this);
        }
    }
}
