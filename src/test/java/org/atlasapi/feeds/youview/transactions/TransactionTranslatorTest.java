package org.atlasapi.feeds.youview.transactions;

import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.media.entity.Publisher;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


@Ignore // TODO rewrite this
public class TransactionTranslatorTest {
    
    private Clock clock = new TimeMachine();
    
//    @Test
//    public void testTranslationToAndFromDBObject() {
//        ImmutableSet<String> contentUrls = ImmutableSet.of("contentUrl1", "contentUrl2");
//        Task transaction = new Task("transactionUrl", Publisher.METABROADCAST, clock.now(), contentUrls, createStatus());
//        
//        Task translated = fromDBObject(toDBObject(transaction));
//        
//        assertEquals(transaction.id(), translated.id());
//        assertEquals(transaction.publisher(), translated.publisher());
//        assertEquals(transaction.uploadTime(), translated.uploadTime());
//        assertEquals(transaction.content(), translated.content());
//        assertEquals(transaction.status(), translated.status());
//    }
//
//    private Response createStatus() {
//        return new Response(TransactionStateType.ACCEPTED, "Transaction accepted");
//    }

}
