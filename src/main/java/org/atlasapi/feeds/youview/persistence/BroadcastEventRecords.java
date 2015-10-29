package org.atlasapi.feeds.youview.persistence;

import org.atlasapi.media.entity.simple.Broadcast;
import org.joda.time.LocalDate;

public class BroadcastEventRecords {
    private final String broadcastEventImi;
    private final LocalDate broadcastTransmissionDate;

    private BroadcastEventRecords(Builder builder){
        this.broadcastEventImi = builder.broadcastEventImi;
        this.broadcastTransmissionDate = builder.broadcastTransmissionDate;
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

        public BroadcastEventRecords build(){
            return new BroadcastEventRecords(this);
        }
    }
}
