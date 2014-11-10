package org.atlasapi.feeds.youview.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.feeds.youview.transactions.persistence.MongoTransactionStore;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.TimeMachine;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class MongoTransactionStoreTest {
    
    private static final Publisher publisher = Publisher.METABROADCAST;

    private DateTime time = new DateTime(2012, 1, 1, 0, 0, 0).withZone(DateTimeZones.UTC);
    private Clock clock = new TimeMachine(time);
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    
    private final TransactionStore store = new MongoTransactionStore(mongo);

    @Test
    public void testSavingAndRetrievingTransactionReturnsEqualTransaction() {
        String transactionUrl = "transactionUrl";
        Transaction transaction = createAndStoreTransaction(transactionUrl, ImmutableSet.of("filmUri"), TransactionStateType.ACCEPTED);
        
        Transaction fetched = store.transactionFor(transactionUrl, publisher).get();
        
        assertEquals(transaction, fetched);
        
        assertEquals(transaction.content(), fetched.content());
        assertEquals(transaction.uploadTime(), fetched.uploadTime());
        assertEquals(transaction.status(), fetched.status());
    }

    @Test
    public void testRetrievalBySameTransactionUrlAndDifferentPublisherDoesNotReturnSameTransaction() {
        String transactionUrl = "transactionUrl";
        createAndStoreTransaction(transactionUrl, ImmutableSet.of("filmUri"), TransactionStateType.ACCEPTED);
        
        Optional<Transaction> fetched = store.transactionFor(transactionUrl, Publisher.BBC);
        
        assertFalse("querying by same transaction url and different publisher shouldn't resolve transaction", fetched.isPresent());
    }
    
    @Test
    public void testUpdatingTransactionWithStatusUpdatesStatusCorrectly() {
        String transactionUrl = "transactionUrl";
        TransactionStatus initialStatus = createStatus(TransactionStateType.ACCEPTED);
        Transaction transaction = createTransaction(transactionUrl, ImmutableSet.of("filmUri"), initialStatus);
        store.save(transaction);
        
        Transaction fetched = store.transactionFor(transactionUrl, publisher).get();
        
        assertEquals(initialStatus, fetched.status());
        
        TransactionStatus updatedStatus = createStatus(TransactionStateType.COMMITTED);
        store.updateWithStatus(transactionUrl, publisher, updatedStatus);
        
        fetched = store.transactionFor(transactionUrl, publisher).get();
        
        assertEquals(updatedStatus, fetched.status());
    }
    
    @Test
    public void testLimitAndOffsetQueryCorrectlyTrimsReturnedSetOfTransactions() {
        Transaction t1 = createAndStoreTransaction("t1", ImmutableSet.<String>of(), TransactionStateType.ACCEPTED);
        Transaction t2 = createAndStoreTransaction("t2", ImmutableSet.<String>of(), TransactionStateType.ACCEPTED);
        Transaction t3 = createAndStoreTransaction("t3", ImmutableSet.<String>of(), TransactionStateType.ACCEPTED);
        
        Selection selection = selectionWithParams(100, 0);
        TransactionQuery query = TransactionQuery.builder(selection, publisher).build();
        Iterable<Transaction> allTransactions = store.allTransactions(query);
        
        assertEquals(ImmutableList.of(t1, t2, t3), ImmutableList.copyOf(allTransactions));
        
        selection = selectionWithParams(1, 1);
        query = TransactionQuery.builder(selection, publisher).build();
        allTransactions = store.allTransactions(query);
        
        assertEquals(Iterables.getOnlyElement(allTransactions), t2);
    }
    
    @Test
    public void testContentUriQueryCorrectlyTrimsReturnedSetOfTransactions() {
        
        String desiredUri = "filterByMe";
        String contentUri = "not part of filter";
        String anotherContentUri = "also not part of filter";
        
        Transaction t1 = createAndStoreTransaction("t1", ImmutableSet.of(desiredUri), TransactionStateType.ACCEPTED);
        Transaction t2 = createAndStoreTransaction("t2", ImmutableSet.of(desiredUri, contentUri), TransactionStateType.ACCEPTED);
        createAndStoreTransaction("t3", ImmutableSet.of(contentUri, anotherContentUri), TransactionStateType.ACCEPTED);
        
        Selection selection = selectionWithParams(100, 0);
        TransactionQuery query = TransactionQuery.builder(selection, publisher)
                .withContentUri(desiredUri)
                .build();
        Iterable<Transaction> allTransactions = store.allTransactions(query);
        
        assertEquals(ImmutableSet.of(t1, t2), ImmutableSet.copyOf(allTransactions));
    }
    
    @Test
    public void testTransactionIdQueryCorrectlyTrimsReturnedSetOfTransactions() {

        Transaction t1 = createAndStoreTransaction("t1", ImmutableSet.<String>of(), TransactionStateType.ACCEPTED);
        createAndStoreTransaction("t2", ImmutableSet.<String>of(), TransactionStateType.ACCEPTED);
        
        Selection selection = selectionWithParams(100, 0);
        TransactionQuery query = TransactionQuery.builder(selection, publisher)
                .withTransactionId(t1.id())
                .build();
        Iterable<Transaction> allTransactions = store.allTransactions(query);
        
        assertEquals(t1, Iterables.getOnlyElement(allTransactions));
    }
    
    @Test
    public void testTransactionStatusQueryCorrectlyTrimsReturnedSetOfTransactions() {

        Transaction t1 = createAndStoreTransaction("t1", ImmutableSet.<String>of(), TransactionStateType.ACCEPTED);
        createAndStoreTransaction("t2", ImmutableSet.<String>of(), TransactionStateType.COMMITTED);
        
        Selection selection = selectionWithParams(100, 0);
        TransactionQuery query = TransactionQuery.builder(selection, publisher)
                .withTransactionStatus(t1.status().status())
                .build();
        Iterable<Transaction> allTransactions = store.allTransactions(query);
        
        assertEquals(t1, Iterables.getOnlyElement(allTransactions));
    }
    
    private Transaction createAndStoreTransaction(String txnUrl, Set<String> content, TransactionStateType status) {
        Transaction txn = createTransaction(
                txnUrl, 
                content, 
                createStatus(status)
        );
        store.save(txn);
        return txn;
    }

    private Selection selectionWithParams(int limit, int offset) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        Mockito.when(request.getParameter("limit")).thenReturn(String.valueOf(limit));
        Mockito.when(request.getParameter("offset")).thenReturn(String.valueOf(offset));
        
        return Selection.builder().build(request);
    }

    private Transaction createTransaction(String transactionUrl, Set<String> content,
            TransactionStatus status) {
        return new Transaction(transactionUrl, publisher, clock.now(), content, status);
    }
    
    private TransactionStatus createStatus(TransactionStateType state) {
        return new TransactionStatus(state, "Fragment accepted, pending processing");
    }
}