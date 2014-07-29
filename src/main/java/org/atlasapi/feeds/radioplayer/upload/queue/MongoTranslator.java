package org.atlasapi.feeds.radioplayer.upload.queue;

import com.mongodb.DBObject;


public interface MongoTranslator<T> {

    DBObject toDBObject(T instance);
        
    T fromDBObject(DBObject dbo);
}
