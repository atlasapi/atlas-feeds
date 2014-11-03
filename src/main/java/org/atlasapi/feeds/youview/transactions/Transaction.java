package org.atlasapi.feeds.youview.transactions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;


public class Transaction {

    private final String id;
    private final Publisher publisher;
    private final DateTime uploadTime;
    private final Set<String> content;
    private final TransactionStatus status;
    
    public Transaction(String id, Publisher publisher, DateTime uploadTime, Set<String> content, TransactionStatus status) {
        this.id = checkNotNull(id);
        this.publisher = checkNotNull(publisher);
        this.uploadTime = checkNotNull(uploadTime);
        this.content = ImmutableSet.copyOf(content);
        this.status = checkNotNull(status);
    }
    
    public String id() {
        return id;
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public DateTime uploadTime() {
        return uploadTime;
    }
    
    public Set<String> content() {
        return content;
    }
    
    public TransactionStatus status() {
        return status;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", id)
                .add("publisher", publisher)
                .add("uploadTime", uploadTime)
                .add("content", content)
                .add("status", status)
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
