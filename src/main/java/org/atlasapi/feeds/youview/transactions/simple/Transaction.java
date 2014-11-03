package org.atlasapi.feeds.youview.transactions.simple;

import java.util.Date;
import java.util.Set;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class Transaction {

    private String id;
    private Publisher publisher;
    private Date uploadTime;
    private Set<String> content;
    private TransactionStateType status;
    private StatusDetail statusDetail;
    
    public Transaction() {
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
    
    public Set<String> content() {
        return content;
    }
    
    public void setContent(Iterable<String> content) {
        this.content = ImmutableSet.copyOf(content);
    }
    
    public TransactionStateType status() {
        return status;
    }
    
    public void setStatus(TransactionStateType status) {
        this.status = status;
    }
    
    public StatusDetail statusDetail() {
        return statusDetail;
    }
    
    public void setStatusDetail(StatusDetail statusDetail) {
        this.statusDetail = statusDetail;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", id)
                .add("publisher", publisher)
                .add("uploadTime", uploadTime)
                .add("content", content)
                .add("status", status)
                .add("statusDetail", statusDetail)
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
        
        if (that instanceof Transaction) {
            Transaction other = (Transaction) that;
            return id.equals(other.id);
        }
        
        return false;
    }
}
