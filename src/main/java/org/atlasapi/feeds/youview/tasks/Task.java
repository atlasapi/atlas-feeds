package org.atlasapi.feeds.youview.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;


public class Task {

    private Long id;
    private final Publisher publisher;
    private final Action action;
    private final Optional<DateTime> uploadTime;
    private final Optional<String> remoteId;
    private final String content;
    private final Status status;
    private final List<Response> remoteResponses;
    
    public static Builder builder() {
        return new Builder();
    }
    
    private Task(Long id, Publisher publisher, Action action, Optional<DateTime> uploadTime, 
            Optional<String> remoteId, String content, Status status, Iterable<Response> remoteResponses) {
        this.id = id;
        this.publisher = checkNotNull(publisher);
        this.action = checkNotNull(action);
        this.uploadTime = checkNotNull(uploadTime);
        this.remoteId = checkNotNull(remoteId);
        this.content = checkNotNull(content);
        this.status = checkNotNull(status);
        this.remoteResponses = ImmutableList.copyOf(remoteResponses);
    }
    
    public Long id() {
        return id;
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
    
    public Optional<DateTime> uploadTime() {
        return uploadTime;
    }
    
    public Optional<String> remoteId() {
        return remoteId;
    }
    
    public String content() {
        return content;
    }
    
    public Status status() {
        return status;
    }
    
    public List<Response> remoteResponses() {
        return remoteResponses;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", id)
                .add("publisher", publisher)
                .add("action", action)
                .add("uploadTime", uploadTime)
                .add("remoteId", remoteId)
                .add("content", content)
                .add("status", status)
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
        private Publisher publisher;
        private Action action;
        private Optional<DateTime> uploadTime = Optional.absent();
        private Optional<String> remoteId = Optional.absent();
        private String content;
        private Status status;
        private ImmutableList.Builder<Response> remoteResponses = ImmutableList.builder();
        
        private Builder() {
        }
        
        public Task build() {
            return new Task(id, publisher, action, uploadTime, remoteId, content, status, remoteResponses.build());
        }
        
        public Builder withId(Long id) {
            this.id = id;
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
        
        public Builder withUploadTime(DateTime uploadTime) {
            this.uploadTime = Optional.fromNullable(uploadTime);
            return this;
        }
        
        public Builder withRemoteId(String remoteId) {
            this.remoteId = Optional.fromNullable(remoteId);
            return this;
        }

        public Builder withContent(String content) {
            this.content = content;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
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
