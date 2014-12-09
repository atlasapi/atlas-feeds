package org.atlasapi.feeds.youview.tasks.persistence;

import static org.atlasapi.feeds.youview.tasks.persistence.PayloadTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.tasks.persistence.PayloadTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.youview.tasks.Payload;
import org.junit.Test;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class PayloadTranslatorTest {
    
    private Clock clock = new TimeMachine();
    
    @Test
    public void testTranslationToAndFromDBObject() {
        
        Payload payload = createPayload();
        Payload translated = fromDBObject(toDBObject(payload));
        
        assertEquals(payload.payload(), translated.payload());
        assertEquals(payload.created(), translated.created());
    }

    private Payload createPayload() {
        return new Payload("payload", clock.now());
    }
}
