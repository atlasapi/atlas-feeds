package org.atlasapi.feeds.youview.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.metabroadcast.common.query.Selection;


public class TaskQuery {

    private final Selection selection;
    private final Publisher publisher;
    private final Optional<String> contentUri;
    private final Optional<String> remoteId;
    private final Optional<Status> status;
    
    public static Builder builder(Selection selection, Publisher publisher) {
        return new Builder(selection, publisher);
    }
    
    private TaskQuery(Selection selection, Publisher publisher, Optional<String> contentUri, 
            Optional<String> remoteId, Optional<Status> status) {
        this.selection = checkNotNull(selection);
        this.publisher = checkNotNull(publisher);
        this.contentUri = checkNotNull(contentUri);
        this.remoteId = checkNotNull(remoteId);
        this.status = checkNotNull(status);
    }
    
    public Selection selection() {
        return selection;
    }
    
    public Publisher publisher() {
        return publisher;
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
    
    @Override
    public String toString() {
        return Objects.toStringHelper(TaskQuery.class)
                .add("selection", selection)
                .add("publisher", publisher)
                .add("contentUri", contentUri)
                .add("remoteId", remoteId)
                .add("status", status)
                .toString();
    }
    
    public static final class Builder {
        
        private final Selection selection;
        private final Publisher publisher;
        private Optional<String> contentUri = Optional.absent();
        private Optional<String> remoteId = Optional.absent();
        private Optional<Status> status = Optional.absent();

        private Builder(Selection selection, Publisher publisher) {
            this.selection = selection;
            this.publisher = publisher;
        }
        
        public TaskQuery build() {
            return new TaskQuery(selection, publisher, contentUri, remoteId, status);
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
    }
}
