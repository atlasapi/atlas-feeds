package org.atlasapi.feeds.youview.tasks.simple;

import java.util.Date;

import org.atlasapi.feeds.youview.tasks.Status;


public class Response implements Comparable<Response> {

    private Status status;
    private String payload;
    private Date created;
    
    public Status status() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
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

    @Override
    public int compareTo(Response that) {
        return this.created.compareTo(that.created);
    }
}
