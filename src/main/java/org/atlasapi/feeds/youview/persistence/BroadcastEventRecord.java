package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Preconditions;
import org.joda.time.LocalDate;

public class BroadcastEventRecord {
    private final String broadcastEventImi;
    private final LocalDate broadcastTransmissionDate;

    private BroadcastEventRecord(Builder builder){
        this.broadcastEventImi = Preconditions.checkNotNull(builder.broadcastEventImi);
        this.broadcastTransmissionDate = Preconditions.checkNotNull(builder.broadcastTransmissionDate);
    }

    public String getBroadcastEventImi() {
        return broadcastEventImi;
    }

    public LocalDate getBroadcastTransmissionDate() {
        return broadcastTransmissionDate;
    }

    public static class Builder{
        private String broadcastEventImi;
        private LocalDate broadcastTransmissionDate;

        public Builder broadcastEventImi(String broadcastEventImi){
            this.broadcastEventImi = broadcastEventImi;
            return this;
        }

        public Builder broadcastTransmissionDate(LocalDate broadcastTransmissionDate){
            this.broadcastTransmissionDate = broadcastTransmissionDate;
            return this;
        }

        public BroadcastEventRecord build(){
            return new BroadcastEventRecord(this);
        }
    }
}
