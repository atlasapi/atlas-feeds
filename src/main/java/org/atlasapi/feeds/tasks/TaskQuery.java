package org.atlasapi.feeds.tasks;

import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.query.Selection;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;


public class TaskQuery {

    public enum Sort {
        ASC,
        DESC
    }

    private final Selection selection;
    private final Publisher publisher;
    private final DestinationType destinationType;
    private final Optional<String> contentUri;
    private final Optional<String> remoteId;
    private final Optional<Status> status;
    private final Optional<Action> action;
    private final Optional<TVAElementType> elementType;
    private final Optional<String> elementId;
    private final Sort sort;
    
    public static Builder builder(Selection selection, Publisher publisher, DestinationType destinationType) {
        return new Builder(selection, publisher, destinationType);
    }
    
    private TaskQuery(
            Selection selection,
            Publisher publisher,
            DestinationType destinationType,
            Optional<String> contentUri,
            Optional<String> remoteId,
            Optional<Status> status,
            Optional<Action> action,
            Optional<TVAElementType> elementType,
            Optional<String> elementId,
            Sort sort
    ) {
        this.selection = checkNotNull(selection);
        this.publisher = checkNotNull(publisher);
        this.destinationType = checkNotNull(destinationType);
        this.contentUri = checkNotNull(contentUri);
        this.remoteId = checkNotNull(remoteId);
        this.status = checkNotNull(status);
        this.action = checkNotNull(action);
        this.elementType = checkNotNull(elementType);
        this.elementId = checkNotNull(elementId);
        this.sort = checkNotNull(sort);
    }
    
    public Selection selection() {
        return selection;
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public DestinationType destinationType() {
        return destinationType;
    }
    
    public Optional<String> contentUri() {
        return contentUri;
    }
    
    public Optional<String> remoteId() {
        return remoteId;
    }
    
    public Optional<Status> status() {
        return status;
    }
    
    public Optional<Action> action() {
        return action;
    }
    
    public Optional<TVAElementType> elementType() {
        return elementType;
    }
    
    public Optional<String> elementId() {
        return elementId;
    }

    public Sort sort() {
        return sort;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(TaskQuery.class)
                .add("selection", selection)
                .add("publisher", publisher)
                .add("destinationType", destinationType)
                .add("contentUri", contentUri)
                .add("remoteId", remoteId)
                .add("status", status)
                .add("action", action)
                .add("elementType", elementType)
                .add("elementId", elementId)
                .add("sort", sort)
                .toString();
    }
    
    public static final class Builder {
        
        private final Selection selection;
        private final Publisher publisher;
        private final DestinationType destinationType;
        private Optional<String> contentUri = Optional.absent();
        private Optional<String> remoteId = Optional.absent();
        private Optional<Status> status = Optional.absent();
        private Optional<Action> action = Optional.absent();
        private Optional<TVAElementType> elementType = Optional.absent();
        private Optional<String> elementId = Optional.absent();
        private Sort sort = Sort.ASC;

        private Builder(Selection selection, Publisher publisher, DestinationType destinationType) {
            this.selection = selection;
            this.publisher = publisher;
            this.destinationType = destinationType;
        }
        
        public TaskQuery build() {
            return new TaskQuery(selection, publisher, destinationType, contentUri, remoteId, 
                    status, action, elementType, elementId, sort);
        }
        
        public Builder withContentUri(String contentUri) {
            this.contentUri = Optional.fromNullable(contentUri);
            return this;
        }
        
        public Builder withRemoteId(String remoteId) {
            this.remoteId = Optional.fromNullable(remoteId);
            return this;
        }
        
        public Builder withTaskStatus(Status status) {
            this.status = Optional.fromNullable(status);
            return this;
        }
        
        public Builder withTaskAction(Action action) {
            this.action = Optional.fromNullable(action);
            return this;
        }
        
        public Builder withTaskType(TVAElementType elementType) {
            this.elementType = Optional.fromNullable(elementType);
            return this;
        }
        
        public Builder withElementId(String elementId) {
            this.elementId = Optional.fromNullable(elementId);
            return this;
        }

        public Builder withSort(Sort sort) {
            this.sort = sort;
            return this;
        }
    }
}
