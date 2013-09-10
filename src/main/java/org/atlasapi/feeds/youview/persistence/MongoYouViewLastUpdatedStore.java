package org.atlasapi.feeds.youview.persistence;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoYouViewLastUpdatedStore implements YouViewLastUpdatedStore {
    
    private static final String ID_VALUE = "YVlastUpdated";
    private static final String LAST_UPDATED_KEY = "lastUpdated";
    private static final String COLLECTION_NAME = "youViewLastUpdated";
    private final DBCollection collection;
    
    public MongoYouViewLastUpdatedStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION_NAME);
    }

    /**
     * ensure that there is a single key used, so all updates replace the same value, so
     * duplicates should never occur.
     */
    @Override
    public void setLastUpdated(DateTime lastUpdated) {
        DBObject dbo = new BasicDBObject();
        TranslatorUtils.from(dbo, MongoConstants.ID, ID_VALUE);
        TranslatorUtils.fromDateTime(dbo, LAST_UPDATED_KEY, lastUpdated);
        collection.update(new BasicDBObject(MongoConstants.ID, ID_VALUE), dbo, true, false);
    }

    @Override
    public Optional<DateTime> getLastUpdated() {
        DBCursor cursor = collection.find();
        if (Iterables.isEmpty(cursor)) {
            return Optional.absent();
        }
        DBObject lastUpdated = Iterables.getOnlyElement(cursor);
        return Optional.of(TranslatorUtils.toDateTime(lastUpdated, LAST_UPDATED_KEY));
    }

}
