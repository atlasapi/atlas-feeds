package org.atlasapi.feeds.youview.tasks.persistence;

import org.atlasapi.feeds.youview.tasks.Payload;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public final class PayloadTranslator {

    private static final String PAYLOAD_KEY = "payload";
    private static final String CREATED_KEY = "created";

    private PayloadTranslator() {
        // private constructor for factory class
    }
    
    public static final DBObject toDBObject(Payload payload) {
        // This makes usage with Optional<Payload> more elegant in the TaskTranslator
        if (payload == null) {
            return null;
        }
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, PAYLOAD_KEY, payload.payload());
        TranslatorUtils.fromDateTime(dbo, CREATED_KEY, payload.created());
        
        return dbo;
    }
    
    public static final Payload fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        
        return new Payload(
                TranslatorUtils.toString(dbo, PAYLOAD_KEY), 
                TranslatorUtils.toDateTime(dbo, CREATED_KEY)
        );
    }
}
