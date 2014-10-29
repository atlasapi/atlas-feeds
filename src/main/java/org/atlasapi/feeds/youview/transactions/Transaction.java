package org.atlasapi.feeds.youview.transactions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;


public class Transaction {

    private final String transactionUrl;
    private final Set<String> contentUrls;
    private final TransactionStatus status;
    
    public static Transaction success(Transaction transaction) {
        return new Transaction(transaction.url(), transaction.contentUrls(), TransactionStatus.SUCCESS);
    }
    
    public static Transaction failure(Transaction transaction) {
        return new Transaction(transaction.url(), transaction.contentUrls(), TransactionStatus.FAILURE);
    }
    
    public Transaction(String transactionUrl, Iterable<String> contentUrls) {
        this(transactionUrl, contentUrls, TransactionStatus.UNKNOWN);
    }
    
    public Transaction(String transactionUrl, Iterable<String> contentUrls, TransactionStatus status) {
        this.transactionUrl = checkNotNull(transactionUrl);
        this.contentUrls = ImmutableSet.copyOf(contentUrls);
        this.status = checkNotNull(status);
    }
    
    public String url() {
        return transactionUrl;
    }
    
    public Set<String> contentUrls() {
        return contentUrls;
    }
    
    public TransactionStatus status() {
        return status;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("transactionUrl", transactionUrl)
                .add("contentUrls", contentUrls)
                .add("status", status)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(transactionUrl);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        
        if (that instanceof Transaction) {
            Transaction other = (Transaction) that;
            return transactionUrl.equals(other.transactionUrl);
        }
        
        return false;
    }
}
