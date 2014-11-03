package org.atlasapi.feeds.youview.transactions;

import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;


public class TransactionTranslatorTest {
    
    // TODO fix this when there's more time
    @Ignore
    @Test
    public void testTranslationToAndFromDBObject() {
        ImmutableSet<String> contentUrls = ImmutableSet.of("contentUrl1", "contentUrl2");
//        Transaction transaction = new Transaction("transactionUrl", contentUrls, TransactionStatusType.SUCCESS);
        
//        Transaction translated = fromDBObject(toDBObject(transaction));
//        
//        assertEquals(transaction.id(), translated.id());
//        assertEquals(transaction.contentLatencies(), translated.contentLatencies());
//        assertEquals(transaction.status(), translated.status());
    }

}
