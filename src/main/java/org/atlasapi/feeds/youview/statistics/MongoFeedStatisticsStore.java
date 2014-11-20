package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.statistics.FeedStatisticsTranslator.QUEUE_SIZE_KEY;
import static org.atlasapi.feeds.youview.statistics.FeedStatisticsTranslator.UPDATE_LATENCY_KEY;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.mongo.MongoUpdateBuilder;
import com.metabroadcast.common.time.Clock;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MongoFeedStatisticsStore implements FeedStatisticsStore {
    
    private static final String COLLECTION_NAME = "youviewFeedStatistics";
    
    private final DBCollection collection;
    private final FeedStatisticsTranslator translator;

    public MongoFeedStatisticsStore(DatabasedMongo mongo, Clock clock) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
        this.translator = new FeedStatisticsTranslator(checkNotNull(clock));
    }

    @Override
    public Optional<FeedStatistics> resolveFor(Publisher publisher) {
        DBObject idQuery = idQuery(publisher);
        return Optional.fromNullable(translator.fromDBObject(collection.findOne(idQuery)));
    }
    
    private DBObject idQuery(Publisher publisher) {
        return new MongoQueryBuilder()
                .idEquals(publisher.name())
                .build();
    }

    @Override
    public void save(FeedStatistics feedStatistics) {
        collection.save(translator.toDBObject(feedStatistics));
    }

    @Override
    public void updateQueueSize(Publisher publisher, int queueSize) {
        DBObject setQueueSize = new MongoUpdateBuilder()
                .setField(QUEUE_SIZE_KEY, queueSize)
                .build();
        collection.update(idQuery(publisher), setQueueSize);
    }

    @Override
    public void updateAverageLatency(Publisher publisher, Duration latency) {
        DBObject setQueueSize = new MongoUpdateBuilder()
        // TODO this leaks the abstraction over duration that translatorUtils gives, but needs must. Fix this.
                .setField(UPDATE_LATENCY_KEY, latency.toString())
                .build();
        collection.update(idQuery(publisher), setQueueSize);
    }
}
