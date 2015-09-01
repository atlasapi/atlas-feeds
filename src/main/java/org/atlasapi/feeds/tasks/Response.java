package org.atlasapi.feeds.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.DateTime;

import com.google.common.base.Objects;

public final class Response {

    private final Status status;
    private final String payload;
    private final DateTime created;
    
    public Response(Status status, String payload, DateTime created) {
        this.status = checkNotNull(status);
        this.payload = checkNotNull(payload);
        this.created = checkNotNull(created);
    }
    
    public Status status() {
        return status;
    }
    
    public String payload() {
        return payload;
    }
    
    public DateTime created() {
        return created;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(Response.class)
                .add("status", status)
                .add("payload", payload)
                .add("created", created)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(status, payload, created);
    }
    
    /**
     * Equality based on Status means that repeated checks of a transaction won't cause
     * multiple responses without a change of state on the remote side
     */
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Response)) {
            return false;
        }
        Response other = (Response) that;
        return status.equals(other.status);
    }
}
