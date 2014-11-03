package org.atlasapi.feeds.youview.transactions.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionStatusTranslator.STATUS_KEY;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.CONTENT_KEY;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.PUBLISHER_KEY;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.UPLOAD_TIME_KEY;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.idFrom;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.toDBObject;

import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.feeds.youview.transactions.TransactionQuery;
import org.atlasapi.feeds.youview.transactions.TransactionStatus;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.mongo.MongoSortBuilder;
import com.metabroadcast.common.persistence.mongo.MongoUpdateBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;


public class MongoTransactionStore implements TransactionStore {
    
    private static final String COLLECTION_NAME = "youviewTransactions";
    
    private final DBCollection collection;
    
    public MongoTransactionStore(DatabasedMongo mongo) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
    }
    
    @Override
    public void save(Transaction transaction) {
        collection.save(toDBObject(transaction));
    }

    @Override
    public boolean updateWithStatus(String transactionId, Publisher publisher, TransactionStatus status) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(idFrom(transactionId, publisher))
                .build();
        DBObject updateStatus = new MongoUpdateBuilder()
                .setField(STATUS_KEY, TransactionStatusTranslator.toDBObject(status))
                .build();
        
        WriteResult result = collection.update(idQuery, updateStatus, false, false);
        return result.getN() == 1;
    }

    @Override
    public Optional<Transaction> transactionFor(String transactionId, Publisher publisher) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(idFrom(transactionId, publisher))
                .build();
        return Optional.fromNullable(fromDBObject(collection.findOne(idQuery)));
    }

    @Override
    public Iterable<Transaction> allTransactions(TransactionQuery query) {
        MongoQueryBuilder mongoQuery = new MongoQueryBuilder();
        
        mongoQuery.fieldEquals(PUBLISHER_KEY, query.publisher().key());
        
        if (query.contentUri().isPresent()) {
            mongoQuery.fieldEquals(CONTENT_KEY, query.transactionStatus().get().name());
        }
        if (query.transactionId().isPresent()) {
            mongoQuery.idEquals(idFrom(query.transactionId().get(), query.publisher()));
        }
        if (query.transactionStatus().isPresent()) {
            mongoQuery.fieldEquals(STATUS_KEY + "." + STATUS_KEY, query.transactionStatus().get().name());
        }
        
        DBCursor cursor = getOrderedCursor(mongoQuery.build())
                .skip(query.selection().getOffset())
                .limit(query.selection().getLimit());
        
        return FluentIterable.from(cursor)
                .transform(TransactionTranslator.fromDBObjects())
                .filter(Predicates.notNull());
    }

    private DBCursor getOrderedCursor(DBObject query) {
        return collection.find(query).sort(new MongoSortBuilder().descending(UPLOAD_TIME_KEY).build());
    }
    
}
