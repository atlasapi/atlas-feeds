package org.atlasapi.feeds.tasks.simple;

import java.util.Date;


public class Payload {

    private String payload;
    private Date created;

    public String payload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Date created() {
        return created;
    }
    
    public void setCreated(Date created) {
        this.created = created;
    }
}
