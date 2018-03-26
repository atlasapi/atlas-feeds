package org.atlasapi.feeds.tasks;

import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.query.Selection;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;


public class TaskQuery {

    public static class Sort {

        public static final Sort DEFAULT = Sort.of(Field.CREATED_TIME, Direction.ASC);
        public static final Sort DATE_ASC = DEFAULT;

        private final Direction direction;
        private final Field field;

        private Sort(Field field, Direction direction) {
            this.direction = direction;
            this.field = field;
        }

        public static Sort of(Field field) {
            return of(field, Direction.ASC);
        }

        public static Sort of(Field field, Direction direction) {
            return new Sort(field, direction);
        }

        public Direction getDirection() {
            return direction;
        }

        public Field getField() {
            return field;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Sort sort = (Sort) o;
            return direction == sort.direction &&
                    field == sort.field;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(direction, field);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("direction", direction)
                    .add("field", field)
                    .toString();
        }

        public enum Direction {
            ASC,
            DESC,
        }

        public enum Field {
            CREATED_TIME("created"),
            TX_TYPE("type", "action"),
            CONTENT_URI("content_uri", "content"),
            YOUVIEW_TX_ID("youview_id", "remoteId"),
            UPLOAD_TIME("uploaded", "uploadTime"),
            STATUS("status"),
            ;

            private final String key;
            private final String dbField;

            Field(String dbField) {
                this(dbField, dbField);
            }

            Field(String key, String dbField) {
                this.key = key;
                this.dbField = dbField;
            }

            public static Field fromKey(String key) {
                for (Field field : values()) {
                    if (key.equals(field.key)) {
                        return field;
                    }
                }
                throw new IllegalArgumentException(String.format("No Field for key %s", key));
            }

            public String getKey() {
                return key;
            }

            public String getDbField() {
                return dbField;
            }
        }
    }

    private final Selection selection;
    private final DestinationType destinationType;
    private final Optional<Publisher> publisher;
    private final Optional<String> contentUri;
    private final Optional<String> remoteId;
    private final Optional<Status> status;
    private final Optional<Action> action;
    private final Optional<TVAElementType> elementType;
    private final Optional<String> elementId;
    private final Optional<DateTime> after;
    private final Sort sort;

    public static Builder builder(Selection selection, DestinationType destinationType) {
        return new Builder(selection, destinationType);
    }
    
    private TaskQuery(
            Selection selection,
            DestinationType destinationType,
            Optional<Publisher> publisher,
            Optional<String> contentUri,
            Optional<String> remoteId,
            Optional<Status> status,
            Optional<Action> action,
            Optional<TVAElementType> elementType,
            Optional<String> elementId,
            Optional<DateTime> after,
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
        this.after = checkNotNull(after);
        this.sort = checkNotNull(sort);
    }
    
    public Selection selection() {
        return selection;
    }
    
    public Optional<Publisher> publisher() {
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

    public Optional<DateTime> getAfter() {
        return after;
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
                .add("after", after)
                .add("sort", sort)
                .toString();
    }
    
    public static final class Builder {
        
        private final Selection selection;
        private final DestinationType destinationType;
        private Optional<Publisher> publisher = Optional.absent();
        private Optional<String> contentUri = Optional.absent();
        private Optional<String> remoteId = Optional.absent();
        private Optional<Status> status = Optional.absent();
        private Optional<Action> action = Optional.absent();
        private Optional<TVAElementType> elementType = Optional.absent();
        private Optional<String> elementId = Optional.absent();
        private Optional<DateTime> after = Optional.absent();
        private Sort sort = Sort.DEFAULT;

        private Builder(Selection selection, DestinationType destinationType) {
            this.selection = selection;
            this.destinationType = destinationType;
        }
        
        public TaskQuery build() {
            return new TaskQuery(selection, destinationType, publisher, contentUri, remoteId,
                    status, action, elementType, elementId, after, sort
            );
        }

        public Builder withPublisher(Publisher publisher) {
            this.publisher = Optional.fromNullable(publisher);
            return this;
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

        /**
         * Will enforce a greather than this date on the created field of the task.
         * @param after
         * @return
         */
        public Builder after(DateTime after){
            this.after = Optional.fromNullable(after);
            return this;
        }

        public Builder withSort(Sort sort) {
            this.sort = sort;
            return this;
        }
    }
}
