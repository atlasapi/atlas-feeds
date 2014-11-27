package org.atlasapi.feeds.youview.persistence;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoYouViewLastUpdatedStore implements YouViewLastUpdatedStore {
    
    private static final String ID_VALUE = "YVlastUpdated";
    private static final String COLLECTION_NAME = "youViewLastUpdated";
    private final DBCollection collection;
    
    public MongoYouViewLastUpdatedStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION_NAME);
    }

    @Override
    public Optional<DateTime> getLastUpdated(Publisher publisher) {
        DBObject lastUpdated = collection.findOne(new BasicDBObject(MongoConstants.ID, ID_VALUE));
        if (lastUpdated == null) {
            return Optional.absent();
        }
        return Optional.fromNullable(TranslatorUtils.toDateTime(lastUpdated, publisher.name()));
    }

    @Override
    public void setLastUpdated(DateTime lastUpdated, Publisher publisher) {
        DBObject dbo = new BasicDBObject();
        TranslatorUtils.from(dbo, MongoConstants.ID, ID_VALUE);
        TranslatorUtils.fromDateTime(dbo, publisher.name(), lastUpdated);
        collection.update(new BasicDBObject(MongoConstants.ID, ID_VALUE), dbo, true, false);
    }

}
