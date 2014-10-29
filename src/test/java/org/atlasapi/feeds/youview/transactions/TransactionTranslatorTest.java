package org.atlasapi.feeds.youview.transactions;

import static org.atlasapi.feeds.youview.transactions.TransactionTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.transactions.TransactionTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;


public class TransactionTranslatorTest {
    
    @Test
    public void testTranslationToAndFromDBObject() {
        ImmutableSet<String> contentUrls = ImmutableSet.of("contentUrl1", "contentUrl2");
        Transaction transaction = new Transaction("transactionUrl", contentUrls, TransactionStatus.SUCCESS);
        
        Transaction translated = fromDBObject(toDBObject(transaction));
        
        assertEquals(transaction.url(), translated.url());
        assertEquals(transaction.contentUrls(), translated.contentUrls());
        assertEquals(transaction.status(), translated.status());
    }

}
