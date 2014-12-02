package org.atlasapi.feeds.youview.tasks;

import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Publisher;
import org.junit.Test;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class TaskTranslatorTest {
    
    private Clock clock = new TimeMachine();
    
    @Test
    public void testTranslationToAndFromDBObject() {
        
        Task transaction = createTask();
        Task translated = fromDBObject(toDBObject(transaction));
        
        assertEquals(transaction.id(), translated.id());
        assertEquals(transaction.publisher(), translated.publisher());
        assertEquals(transaction.uploadTime(), translated.uploadTime());
        assertEquals(transaction.action(), translated.action());
        assertEquals(transaction.elementType(), translated.elementType());
        assertEquals(transaction.elementId(), translated.elementId());
        assertEquals(transaction.content(), translated.content());
        assertEquals(transaction.remoteId(), translated.remoteId());
        assertEquals(transaction.status(), translated.status());
        assertEquals(transaction.remoteResponses(), translated.remoteResponses());
    }

    private Task createTask() {
        return Task.builder()
                .withId(1234l)
                .withPublisher(Publisher.METABROADCAST)
                .withContent("content")
                .withElementType(TVAElementType.ITEM)
                .withElementId("elementId")
                .withUploadTime(clock.now())
                .withRemoteId("remoteId")
                .withAction(Action.UPDATE)
                .withStatus(Status.ACCEPTED)
                .withRemoteResponse(createResponse())
                .build();
    }

    private Response createResponse() {
        return new Response(Status.ACCEPTED, "payload", clock.now());
    }

}
