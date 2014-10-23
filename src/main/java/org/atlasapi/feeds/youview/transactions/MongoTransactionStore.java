package org.atlasapi.feeds.youview.transactions;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.transactions.TransactionTranslator.STATUS_KEY;
import static org.atlasapi.feeds.youview.transactions.TransactionTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.transactions.TransactionTranslator.toDBObject;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.mongo.MongoUpdateBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MongoTransactionStore implements TransactionStore {
    
    private static final String COLLECTION_NAME = "youviewTransactions";

    private static final Function<Content, String> TO_URL = new Function<Content, String>() {
        @Override
        public String apply(Content input) {
            return input.getCanonicalUri();
        }
    };
    
    private final DBCollection collection;
    
    public MongoTransactionStore(DatabasedMongo mongo, Publisher publisher) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME + publisher.name());
    }

    @Override
    public void save(String transactionUrl, Iterable<Content> content) {
        Transaction trans = new Transaction(transactionUrl, Iterables.transform(content, TO_URL));
        collection.save(toDBObject(trans));
    }

    @Override
    public boolean updateWithStatus(String transactionUrl, TransactionStatus status) {
        DBObject idQuery = new MongoQueryBuilder().idEquals(transactionUrl).build();
        DBObject updateStatus = new MongoUpdateBuilder().setField(STATUS_KEY, status.name()).build();
        collection.update(idQuery, updateStatus, false, false);
        // TODO adjust the boolean return value
        return true;
    }

    @Override
    public Optional<Transaction> transactionFor(String transactionUrl) {
        DBObject idQuery = new MongoQueryBuilder().idEquals(transactionUrl).build();
        return Optional.fromNullable(fromDBObject(collection.findOne(idQuery)));
    }

    @Override
    public Iterable<Transaction> allTransactions() {
        return FluentIterable.from(collection.find())
                .transform(TransactionTranslator.fromDBObjects())
                .filter(Predicates.notNull());
    }

    
}
