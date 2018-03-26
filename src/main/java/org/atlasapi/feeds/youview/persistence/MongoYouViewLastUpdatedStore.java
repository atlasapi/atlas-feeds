package org.atlasapi.feeds.youview.persistence;

import java.util.Date;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.metabroadcast.common.time.DateTimeZones;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoYouViewLastUpdatedStore implements YouViewLastUpdatedStore {

    private static final String COLLECTION_NAME = "youviewLastUpdated";
    private static final String LAST_TASKS_CREATED = "lastTaskCreated";
    private static final String LAST_REP_ID_CHECKED = "lastRepIdChangesChecked";
    private final DBCollection collection;

    public MongoYouViewLastUpdatedStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION_NAME);
    }

    @Override
    public Optional<DateTime> getLastUpdated(Publisher publisher) {
        DBObject lastUpdated = collection.findOne(
                getPublisherQuery(publisher)
        );
        if (lastUpdated == null) {
            return Optional.absent();
        }
        return Optional.fromNullable(TranslatorUtils.toDateTime(lastUpdated, LAST_TASKS_CREATED));
    }

    @Override
    public Optional<DateTime> getLastRepIdChangesChecked(Publisher publisher) {
        DBObject lastUpdated = collection.findOne(getPublisherQuery(publisher));
        if (lastUpdated == null) {
            return Optional.absent();
        }
        return Optional.fromNullable(TranslatorUtils.toDateTime(lastUpdated, LAST_REP_ID_CHECKED));
    }

    @Override
    public void setLastUpdated(DateTime lastUpdated, Publisher publisher) {
        DBObject dateObject = new BasicDBObject();
        TranslatorUtils.fromDateTime(dateObject, LAST_TASKS_CREATED, lastUpdated);
        BasicDBObject set = new BasicDBObject("$set", dateObject);
        collection.update(getPublisherQuery(publisher), set, true, true);
    }

    @Override
    public void setLastRepIdChecked(DateTime lastUpdated, Publisher publisher) {
        DBObject dateObject = new BasicDBObject();
        TranslatorUtils.fromDateTime(dateObject, LAST_REP_ID_CHECKED, lastUpdated);
        BasicDBObject set = new BasicDBObject("$set", dateObject);
        collection.update(getPublisherQuery(publisher), set, true, true);
    }

    private static BasicDBObject getPublisherQuery(Publisher publisher) {
        return new BasicDBObject(MongoConstants.ID, publisher.name());
    }

}
