package org.atlasapi.feeds.youview.transactions;

import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Publisher;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class TransactionTranslatorTest {
    
    private Clock clock = new TimeMachine();
    
    @Test
    public void testTranslationToAndFromDBObject() {
        ImmutableSet<String> contentUrls = ImmutableSet.of("contentUrl1", "contentUrl2");
        Transaction transaction = new Transaction("transactionUrl", Publisher.METABROADCAST, clock.now(), contentUrls, createStatus());
        
        Transaction translated = fromDBObject(toDBObject(transaction));
        
        assertEquals(transaction.id(), translated.id());
        assertEquals(transaction.publisher(), translated.publisher());
        assertEquals(transaction.uploadTime(), translated.uploadTime());
        assertEquals(transaction.content(), translated.content());
        assertEquals(transaction.status(), translated.status());
    }

    private TransactionStatus createStatus() {
        return new TransactionStatus(TransactionStateType.ACCEPTED, "Transaction accepted");
    }

}
