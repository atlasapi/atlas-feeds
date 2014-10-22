package org.atlasapi.feeds.youview.transactions;

import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class MongoTransactionStoreTest {
    
    private static final Function<Transaction, String> TO_URL = new Function<Transaction, String>() {
        @Override
        public String apply(Transaction input) {
            return input.url();
        }
    };

    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private final TransactionStore store = new MongoTransactionStore(mongo);

    @Test
    public void testSavingTransactionUrlAndContentSavesTransactionWithContentUrls() {
        String transactionUrl = "transactionUrl";
        String filmUri = "filmUri";
        store.save(transactionUrl, contentForUri(filmUri));
        
        Transaction fetched = store.transactionFor(transactionUrl).get();
        
        assertEquals(ImmutableSet.of(filmUri), fetched.contentUrls());
    }
    
    @Test
    public void testSavingTransactionUrlAndContentSavesTransactionWithUnknownStatus() {
        String transactionUrl = "transactionUrl";
        store.save(transactionUrl, contentForUri("filmUri"));
        
        Transaction fetched = store.transactionFor(transactionUrl).get();
        
        assertEquals(TransactionStatus.UNKNOWN, fetched.status());
    }

    @Test
    public void testUpdatingTransactionWithStatusUpdatesStatusCorrectly() {
        String transactionUrl = "transactionUrl";
        store.save(transactionUrl, contentForUri("filmUri"));
        
        Transaction fetched = store.transactionFor(transactionUrl).get();
        
        assertEquals(TransactionStatus.UNKNOWN, fetched.status());
        
        TransactionStatus updated = TransactionStatus.SUCCESS;
        store.updateWithStatus(transactionUrl, updated);
        
        fetched = store.transactionFor(transactionUrl).get();
        
        assertEquals(updated, fetched.status());
    }
    
    @Test
    public void testTransactionMultiFetch() {
        String aTransactionUrl = "transactionUrl";
        String anotherTansactionUrl = "anotherTransactionUrl";
        
        store.save(aTransactionUrl, contentForUri("filmUri"));
        store.save(anotherTansactionUrl, contentForUri("anotherUri"));
        
        Iterable<Transaction> fetched = store.allTransactions();
        ImmutableSet<String> fetchedUrls = ImmutableSet.copyOf(Iterables.transform(fetched, TO_URL));
        
        assertEquals(ImmutableSet.of(aTransactionUrl, anotherTansactionUrl), fetchedUrls);
    }

    private ImmutableSet<Content> contentForUri(String uri) {
        return ImmutableSet.<Content>of(new Film(uri, "curie", Publisher.METABROADCAST));
    }
}
