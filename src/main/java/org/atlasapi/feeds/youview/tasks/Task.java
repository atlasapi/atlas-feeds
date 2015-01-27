package org.atlasapi.feeds.youview.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;


public class Task {

    private Long id;
    private final DateTime created;
    private final Publisher publisher;
    private final Action action;
    private final Destination destination;
    private final Status status;
    private final Optional<DateTime> uploadTime;
    private final Optional<String> remoteId;
    private final Optional<Payload> payload;
    private final Set<Response> remoteResponses;
    
    public static Builder builder() {
        return new Builder();
    }
    
    private Task(Long id, DateTime created, Publisher publisher, Action action, Destination destination, Status status, 
            Optional<DateTime> uploadTime, Optional<String> remoteId, Optional<Payload> payload, 
            Iterable<Response> remoteResponses) {
        this.id = id;
        this.created = checkNotNull(created);
        this.publisher = checkNotNull(publisher);
        this.action = checkNotNull(action);
        this.destination = checkNotNull(destination);
        this.status = checkNotNull(status);
        this.uploadTime = checkNotNull(uploadTime);
        this.remoteId = checkNotNull(remoteId);
        this.payload = checkNotNull(payload);
        this.remoteResponses = ImmutableSet.copyOf(remoteResponses);
    }
    
    public Long id() {
        return id;
    }
    
    public DateTime created() {
        return created;
    }
    
    public void setId(Long id) {
        this.id = checkNotNull(id);
    }
    
    public Action action() {
        return action;
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public Destination destination() {
        return destination;
    }
    
    public Status status() {
        return status;
    }
    
    public Optional<DateTime> uploadTime() {
        return uploadTime;
    }
    
    public Optional<String> remoteId() {
        return remoteId;
    }
    
    public Optional<Payload> payload() {
        return payload;
    }
    
    public Set<Response> remoteResponses() {
        return remoteResponses;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", id)
                .add("created", created)
                .add("publisher", publisher)
                .add("action", action)
                .add("destination", destination)
                .add("status", status)
                .add("uploadTime", uploadTime)
                .add("remoteId", remoteId)
                .add("payload", payload)
                .add("remoteResponses", remoteResponses)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        
        if (that instanceof Task) {
            Task other = (Task) that;
            return id.equals(other.id);
        }
        
        return false;
    }
    
    public static class Builder {
        
        private Long id;
        private DateTime created;
        private Publisher publisher;
        private Action action;
        private Destination destination;
        private Status status;
        private Optional<DateTime> uploadTime = Optional.absent();
        private Optional<String> remoteId = Optional.absent();
        private Optional<Payload> payload = Optional.absent();
        private ImmutableSet.Builder<Response> remoteResponses = ImmutableSet.builder();
        
        private Builder() { }
        
        public Task build() {
            return new Task(id, created, publisher, action, destination, status, uploadTime, 
                    remoteId, payload, remoteResponses.build());
        }
        
        public Builder withId(Long id) {
            this.id = id;
            return this;
        }
        
        public Builder withCreated(DateTime created) {
            this.created = created;
            return this;
        }
        
        public Builder withPublisher(Publisher publisher) {
            this.publisher = publisher;
            return this;
        }
        
        public Builder withAction(Action action) {
            this.action = action;
            return this;
        }
        
        public Builder withDestination(Destination destination) {
            this.destination = destination;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }
        
        public Builder withUploadTime(DateTime uploadTime) {
            this.uploadTime = Optional.fromNullable(uploadTime);
            return this;
        }
        
        public Builder withRemoteId(String remoteId) {
            this.remoteId = Optional.fromNullable(remoteId);
            return this;
        }
        
        public Builder withPayload(Payload payload) {
            this.payload = Optional.fromNullable(payload);
            return this;
        }
        
        public Builder withRemoteResponse(Response response) {
            this.remoteResponses.add(response);
            return this;
        }
        
        public Builder withRemoteResponses(Iterable<Response> responses) {
            this.remoteResponses.addAll(responses);
            return this;
        }
    }
}
