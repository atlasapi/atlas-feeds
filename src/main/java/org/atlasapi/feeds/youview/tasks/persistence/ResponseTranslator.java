package org.atlasapi.feeds.youview.tasks.persistence;

import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;

import com.google.common.base.Function;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class ResponseTranslator {

    private static final String STATUS_KEY = "status";
    private static final String PAYLOAD_KEY = "message";
    private static final String CREATED_KEY = "created";
    
    private ResponseTranslator() {
        // private constructor for factory class
    }

    public static DBObject toDBObject(Response response) {
        if (response == null) {
            return null;
        }
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, STATUS_KEY, response.status().name());
        TranslatorUtils.from(dbo, PAYLOAD_KEY, response.payload());
        TranslatorUtils.fromDateTime(dbo, CREATED_KEY, response.created());
        
        return dbo;
    }
    
    public static Function<Response, DBObject> toDBObject() {
        return new Function<Response, DBObject>() {
            @Override
            public DBObject apply(Response input) {
                return toDBObject(input);
            }
        };
    }
    
    public static Response fromDBObject(DBObject dbo) {
        return new Response(
                Status.valueOf(TranslatorUtils.toString(dbo, STATUS_KEY)), 
                TranslatorUtils.toString(dbo, PAYLOAD_KEY), 
                TranslatorUtils.toDateTime(dbo, CREATED_KEY)
        );
    }
    
    public static Function<DBObject, Response> fromDBObject() {
        return new Function<DBObject, Response>() {
            @Override
            public Response apply(DBObject input) {
                return fromDBObject(input);
            }
        };
    }
}
