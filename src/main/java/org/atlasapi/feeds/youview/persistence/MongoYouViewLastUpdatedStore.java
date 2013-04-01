package org.atlasapi.feeds.youview.persistence;

import org.joda.time.DateTime;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoYouViewLastUpdatedStore implements YouViewLastUpdatedStore {
    
    private static final String LAST_UPDATED_KEY = "lastUpdated";
    private static final String COLLECTION_NAME = "youViewLastUpdated";
    private final DBCollection collection;
    
    public MongoYouViewLastUpdatedStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION_NAME);
    }

    @Override
    public void setLastUpdated(DateTime lastUpdated) {
        collection.remove(new BasicDBObject());
        DBObject dbo = new BasicDBObject();
        TranslatorUtils.fromDateTime(dbo, LAST_UPDATED_KEY, lastUpdated);
        collection.save(dbo);
    }

    @Override
    public DateTime getLastUpdated() {
        DBObject lastUpdated = Iterables.getOnlyElement(collection.find());
        return TranslatorUtils.toDateTime(lastUpdated, LAST_UPDATED_KEY);
    }

}