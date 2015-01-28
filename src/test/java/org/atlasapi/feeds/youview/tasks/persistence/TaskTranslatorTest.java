package org.atlasapi.feeds.youview.tasks.persistence;

import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.fromDBObject;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
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
        assertEquals(transaction.destination(), translated.destination());
        assertEquals(transaction.remoteId(), translated.remoteId());
        assertEquals(transaction.status(), translated.status());
        assertEquals(transaction.remoteResponses(), translated.remoteResponses());
    }

    private Task createTask() {
        return Task.builder()
                .withId(1234l)
                .withPublisher(Publisher.METABROADCAST)
                .withDestination(createDestination())
                .withUploadTime(clock.now())
                .withRemoteId("remoteId")
                .withPayload(createPayload())
                .withAction(Action.UPDATE)
                .withStatus(Status.ACCEPTED)
                .withRemoteResponse(createResponse())
                .build();
    }

    private Destination createDestination() {
        return new YouViewDestination("content", TVAElementType.ITEM, "elementId");
    }

    private Payload createPayload() {
        return new Payload("payload", clock.now());
    }

    private Response createResponse() {
        return new Response(Status.ACCEPTED, "payload", clock.now());
    }

}
