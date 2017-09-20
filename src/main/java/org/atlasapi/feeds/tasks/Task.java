package org.atlasapi.feeds.tasks;

import java.util.Set;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class Task {

    private Long id;
    private final Long atlasDbId;
    private final DateTime created;
    private final Publisher publisher;
    private final Action action;
    private final Destination destination;
    private final Status status;
    private final Optional<DateTime> uploadTime;
    private final Optional<String> remoteId;
    private final Optional<Payload> payload;
    private final ImmutableSet<Response> remoteResponses;
    private final Optional<String> lastError;
    private final Boolean manuallyCreated;
    private final Optional<String> entityType;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder copy(Task task) {
        Builder builder = builder()
                .withCreated(task.created)
                .withAtlasDbId(task.atlasDbId)
                .withPublisher(task.publisher)
                .withAction(task.action)
                .withDestination(task.destination)
                .withStatus(task.status)
                .withUploadTime(task.uploadTime.orNull())
                .withRemoteId(task.remoteId.orNull())
                .withPayload(task.payload.orNull())
                .withEntityType(task.entityType.orNull())
                .withLastError(task.lastError.orNull())
                .withManuallyCreated(task.manuallyCreated);
        for (Response remoteResponse : task.remoteResponses) {
            builder.withRemoteResponse(remoteResponse);
        }
        return builder;
    }

    private Task(
            Long id, Long atlasDbId, DateTime created, Publisher publisher, Action action,
            Destination destination, Status status,
            Optional<DateTime> uploadTime,
            Optional<String> remoteId,
            Optional<Payload> payload,
            Optional<String> entityType,
            Iterable<Response> remoteResponses,
            Optional<String> lastError,
            Boolean manuallyCreated
    ) {
        this.id = id;
        this.atlasDbId = atlasDbId;
        this.created = checkNotNull(created);
        this.publisher = checkNotNull(publisher);
        this.action = checkNotNull(action);
        this.destination = checkNotNull(destination);
        this.status = checkNotNull(status);
        this.uploadTime = checkNotNull(uploadTime);
        this.remoteId = checkNotNull(remoteId);
        this.payload = checkNotNull(payload);
        this.entityType = checkNotNull(entityType);
        this.remoteResponses = ImmutableSet.copyOf(remoteResponses);
        this.lastError = checkNotNull(lastError);
        this.manuallyCreated = manuallyCreated;
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

    public Long atlasDbId() { return atlasDbId; }
    
    public Action action() {
        return action;
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public Destination destination() {
        return destination;
    }
    
    public Optional<String> lastError() {
        return lastError;
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

    public Optional<String> entityType() { return entityType; }
    
    public Set<Response> remoteResponses() {
        return remoteResponses;
    }

    public Boolean isManuallyCreated() {
        return manuallyCreated;
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
                .add("lastError", lastError)
                .add("manuallyCreated", manuallyCreated)
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
        private Long atlasDbId;
        private DateTime created;
        private Publisher publisher;
        private Action action;
        private Destination destination;
        private Status status;
        private Optional<DateTime> uploadTime = Optional.absent();
        private Optional<String> remoteId = Optional.absent();
        private Optional<Payload> payload = Optional.absent();
        private Optional<String> lastError = Optional.absent();
        private ImmutableSet.Builder<Response> remoteResponses = ImmutableSet.builder();
        private Boolean manuallyCreated = Boolean.FALSE;
        private Optional<String> entityType = Optional.absent();

        private Builder() { }
        
        public Task build() {
            return new Task(id,
                    atlasDbId, created, publisher, action, destination, status, uploadTime,
                    remoteId, payload, entityType, remoteResponses.build(), lastError, manuallyCreated);
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withAtlasDbId(Long atlasDbId){
            this.atlasDbId = atlasDbId;
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
        
        public Builder withLastError(String lastError) {
            this.lastError = Optional.fromNullable(lastError);
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

        public Builder withManuallyCreated(Boolean manuallyCreated) {
            this.manuallyCreated = manuallyCreated != null && manuallyCreated;
            return this;
        }

        public Builder withEntityType(String entityType) {
            this.entityType = Optional.fromNullable(entityType);
            return this;
        }
    }
}
