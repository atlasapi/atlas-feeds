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

    private static final String COLLECTION_NAME = "youViewLastUpdated";
    private final DBCollection collection;
    
    public MongoYouViewLastUpdatedStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION_NAME);
    }

    @Override
    public Optional<DateTime> getLastUpdated(Publisher publisher) {
        DBObject lastUpdated = collection.findOne(new BasicDBObject(MongoConstants.ID, publisher.key()));
        if (lastUpdated == null) {
            return Optional.absent();
        }
        return Optional.fromNullable(TranslatorUtils.toDateTime(lastUpdated, "date"));
    }

    @Override
    public void setLastUpdated(DateTime lastUpdated, Publisher publisher) {
        DBObject dbo = new BasicDBObject();
        TranslatorUtils.fromDateTime(dbo, "date", lastUpdated);
        collection.update(new BasicDBObject(MongoConstants.ID, publisher.key()), dbo, true, false);
    }

}
