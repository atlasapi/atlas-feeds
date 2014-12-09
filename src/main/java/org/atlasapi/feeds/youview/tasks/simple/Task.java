package org.atlasapi.feeds.youview.tasks.simple;

import java.util.Date;
import java.util.List;

import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;


public class Task {

    private String id;
    private Publisher publisher;
    private Date uploadTime;
    private Action action;
    private String remoteId;
    private Payload payload;
    private TVAElementType elementType;
    private String elementId;
    private String content;
    private Status status;
    private List<Response> remoteResponses;
    
    public Task() {
    }
    
    public String id() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }
    
    public Date uploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public Action action() {
        return action;
    }
    
    public void setAction(Action action) {
        this.action = action;
    }
    
    public String remoteId() {
        return remoteId;
    }
    
    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }
    
    public Payload payload() {
        return payload;
    }
    
    public void setPayload(Payload payload) {
        this.payload = payload;
    }
    
    public TVAElementType elementType() {
        return elementType;
    }
    
    public void setElementType(TVAElementType elementType) {
        this.elementType = elementType;
    }
    
    public String elementId() {
        return elementId;
    }
    
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }
    
    public String content() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Status status() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public List<Response> remoteResponses() {
        return remoteResponses;
    }
    
    public void setRemoteResponses(Iterable<Response> remoteResponses) {
        this.remoteResponses = ImmutableList.copyOf(remoteResponses);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", id)
                .add("publisher", publisher)
                .add("uploadTime", uploadTime)
                .add("action", action)
                .add("remoteId", remoteId)
                .add("elementType", elementType)
                .add("elementId", elementId)
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
}
