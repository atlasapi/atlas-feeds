package org.atlasapi.feeds.youview.transactions.persistence;

import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.feeds.youview.transactions.TransactionQuery;
import org.atlasapi.feeds.youview.transactions.TransactionStatus;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;


public interface TransactionStore {
    
    void save(Transaction transaction);
    
    /**
     * Updates the record for a given transaction with a status. Returns true if the update
     * occurs, and false if the transaction does not exist.
     * @param transactionId the url of the transaction to be updated
     * @param status the status returned from YouView for the provided transaction
     * @return
     */
    boolean updateWithStatus(String transactionId, Publisher publisher, TransactionStatus status);
    
    Optional<Transaction> transactionFor(String transactionId, Publisher publisher);
    
    Iterable<Transaction> allTransactions(TransactionQuery query);
}
