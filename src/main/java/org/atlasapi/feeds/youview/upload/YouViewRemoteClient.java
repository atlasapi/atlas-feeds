package org.atlasapi.feeds.youview.upload;

import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.feeds.youview.transactions.TransactionStatus;
import org.atlasapi.media.entity.Content;


public interface YouViewRemoteClient {

    Transaction upload(Content content);

    boolean sendDeleteFor(Content content);
    
    TransactionStatus checkRemoteStatusOf(Transaction transaction);

}
