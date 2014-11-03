package org.atlasapi.feeds.youview.transactions;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.youview.transactions.persistence.MongoTransactionStore;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


// TODO re-enable and fix when have more time
@Ignore
public class MongoTransactionStoreTest {
    
    private static final Function<Transaction, String> TO_URL = new Function<Transaction, String>() {
        @Override
        public String apply(Transaction input) {
            return input.id();
        }
    };

    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private final TransactionStore store = new MongoTransactionStore(mongo);

    @Test
    public void testSavingTransactionUrlAndContentSavesTransactionWithContentUrls() {
        String transactionUrl = "transactionUrl";
        String filmUri = "filmUri";
//        store.save(transactionUrl, contentForUri(filmUri));
//        
//        Transaction fetched = store.transactionFor(transactionUrl).get();
//        
//        assertEquals(ImmutableSet.of(filmUri), fetched.contentLatencies());
    }
    
    @Test
    public void testSavingTransactionUrlAndContentSavesTransactionWithUnknownStatus() {
        String transactionUrl = "transactionUrl";
//        store.save(transactionUrl, contentForUri("filmUri"));
//        
//        Transaction fetched = store.transactionFor(transactionUrl).get();
//        
//        assertEquals(TransactionStatusType.PENDING, fetched.status());
    }

    @Test
    public void testUpdatingTransactionWithStatusUpdatesStatusCorrectly() {
        String transactionUrl = "transactionUrl";
//        store.save(transactionUrl, contentForUri("filmUri"));
//        
//        Transaction fetched = store.transactionFor(transactionUrl).get();
//        
//        assertEquals(TransactionStatusType.PENDING, fetched.status());
//        
//        TransactionStatusType updated = TransactionStatusType.SUCCESS;
//        store.updateWithStatus(transactionUrl, updated);
//        
//        fetched = store.transactionFor(transactionUrl).get();
//        
//        assertEquals(updated, fetched.status());
    }
    
    @Test
    public void testTransactionMultiFetch() {
        String aTransactionUrl = "transactionUrl";
//        String anotherTansactionUrl = "anotherTransactionUrl";
//        
//        store.save(aTransactionUrl, contentForUri("filmUri"));
//        store.save(anotherTansactionUrl, contentForUri("anotherUri"));
//        
//        Iterable<Transaction> fetched = store.allTransactions();
//        ImmutableSet<String> fetchedUrls = ImmutableSet.copyOf(Iterables.transform(fetched, TO_URL));
//        
//        assertEquals(ImmutableSet.of(aTransactionUrl, anotherTansactionUrl), fetchedUrls);
    }

    private ImmutableSet<Content> contentForUri(String uri) {
        return ImmutableSet.<Content>of(new Film(uri, "curie", Publisher.METABROADCAST));
    }
}
