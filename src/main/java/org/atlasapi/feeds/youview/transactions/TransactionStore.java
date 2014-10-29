package org.atlasapi.feeds.youview.transactions;

import org.atlasapi.media.entity.Content;

import com.google.common.base.Optional;


public interface TransactionStore {

    void save(String transactionUrl, Iterable<Content> content);
    
    /**
     * Updates the record for a given transaction with a status. Returns true if the update
     * occurs, and false if the transaction does not exist.
     * @param transactionUrl the url of the transaction to be updated
     * @param status the status returned from YouView for the provided transaction
     * @return
     */
    // TODO does the status need to contain more information? review once status information is clear
    boolean updateWithStatus(String transactionUrl, TransactionStatus status);
    
    Optional<Transaction> transactionFor(String transactionUrl);
    
    Iterable<Transaction> allTransactions();
}
