package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.statistics.FeedStatisticsTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.statistics.FeedStatisticsTranslator.toDBObject;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MongoFeedStatisticsStore implements FeedStatisticsStore {
    
    private static final String COLLECTION_NAME = "youviewFeedStatistics";
    
    private final DBCollection collection;

    public MongoFeedStatisticsStore(DatabasedMongo mongo) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
    }

    @Override
    public Optional<FeedStatistics> resolveFor(Publisher publisher) {
        DBObject idQuery = idQuery(publisher);
        return Optional.fromNullable(fromDBObject(collection.findOne(idQuery)));
    }
    
    private DBObject idQuery(Publisher publisher) {
        return new MongoQueryBuilder()
                .idEquals(publisher.name())
                .build();
    }

    @Override
    public void updateFeedStatistics(Publisher publisher, int queueSize, Duration latency) {
        collection.save(toDBObject(new FeedStatistics(publisher, queueSize, latency)));
    }
}
