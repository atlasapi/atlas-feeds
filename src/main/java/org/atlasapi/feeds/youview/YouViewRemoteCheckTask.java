package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class YouViewRemoteCheckTask extends ScheduledTask {
    
    private static final TransactionStateType UNCHECKED = TransactionStateType.ACCEPTED;
    private final TransactionStore transactionStore;
    private final Publisher publisher;
    private final YouViewRemoteClient client;

    public YouViewRemoteCheckTask(TransactionStore transactionStore, Publisher publisher, YouViewRemoteClient client) {
        this.transactionStore = checkNotNull(transactionStore);
        this.publisher = checkNotNull(publisher);
        this.client = checkNotNull(client);
    }

    @Override
    protected void runTask() {
        Iterable<Transaction> txnsToCheck = transactionStore.allTransactions(UNCHECKED, publisher);
        for (Transaction txn : txnsToCheck) {
            transactionStore.updateWithStatus(txn.id(), publisher, client.checkRemoteStatusOf(txn));
        }
    }
}
