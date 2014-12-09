package org.atlasapi.feeds.youview.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.DateTime;

import com.google.common.base.Objects;


public final class Payload {

    private final String payload;
    private final DateTime created;

    public Payload(String payload, DateTime created) {
        this.payload = checkNotNull(payload);
        this.created = checkNotNull(created);
    }

    public String payload() {
        return payload;
    }

    public DateTime created() {
        return created;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(Payload.class)
                .add("payload", payload)
                .add("created", created)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(payload);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        
        if (!(that instanceof Payload)) {
            return false;
        }
        
        Payload other = (Payload) that;
        return payload.equals(other.payload);
    }
}
