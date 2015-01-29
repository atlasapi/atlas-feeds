package org.atlasapi.feeds.tasks.persistence;

import static org.atlasapi.feeds.tasks.persistence.ResponseTranslator.fromDBObject;
import static org.atlasapi.feeds.tasks.persistence.ResponseTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.junit.Test;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class ResponseTranslatorTest {
    
    private Clock clock = new TimeMachine();
    
    @Test
    public void testTranslationToAndFromDBObjectWhenStatusHasNoReports() {
        
        Response status = new Response(Status.ACCEPTED, "payload", clock.now());
        
        Response translated = fromDBObject(toDBObject(status));
        
        assertEquals(status.status(), translated.status());
        assertEquals(status.payload(), translated.payload());
        assertEquals(status.created(), translated.created());
    }
}
