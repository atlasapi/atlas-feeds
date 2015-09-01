package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.locks.Lock;

import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.metabroadcast.common.time.Clock;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MongoFeedStatisticsStore implements FeedStatisticsStore {
    
    private static final String COLLECTION_NAME = "youViewFeedLatency";
    private static final String LATENCY_SUM_KEY = "sum";
    private static final String LATENCY_COUNT_KEY = "count";
    
    private final Joiner JOIN_ON_PIPE = Joiner.on('|');
    private final DateTimeFormatter DATE_FORMAT = ISODateTimeFormat.date();
    
    private final DBCollection collection;
    private final TaskStore taskStore;
    private final Clock clock;
    private final DestinationType destinationType;
    private final Lock latencyLock;

    public MongoFeedStatisticsStore(DatabasedMongo mongo, TaskStore taskStore, 
            Clock clock, DestinationType destinationType, Lock latencyLock) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
        this.taskStore = checkNotNull(taskStore);
        this.clock = checkNotNull(clock);
        this.destinationType = checkNotNull(destinationType);
        this.latencyLock = checkNotNull(latencyLock);
    }

    @Override
    public Optional<FeedStatistics> resolveFor(Publisher publisher) {
        DBObject idQuery = idQuery(publisher, clock.now());
        
        DBObject result = collection.findOne(idQuery);
        if (result == null) {
            return Optional.absent();
        }
        Duration latency = extractLatency(result);
        int queueSize = calculateCurrentQueueSize();
        
        return Optional.of(new FeedStatistics(publisher, queueSize, latency));
    }

    private Duration extractLatency(DBObject todayLatency) {
        Duration summedDuration = TranslatorUtils.toDuration(todayLatency, LATENCY_SUM_KEY);
        Integer count = TranslatorUtils.toInteger(todayLatency, LATENCY_COUNT_KEY);
        return Duration.standardSeconds(summedDuration.getStandardSeconds() / (long) count);
    }
    
    private int calculateCurrentQueueSize() {
        return Iterables.size(taskStore.allTasks(destinationType, Status.NEW));
    }

    /**
     * Retrieves the latency record with the subset of the latency array for 
     * any dates greater than yesterday (which should be the single latency 
     * record for today)
     * 
     * @param publisher the publisher for which to retrieve a latency object
     */
    private DBObject idQuery(Publisher publisher, DateTime now) {
        return new MongoQueryBuilder()
                .idEquals(JOIN_ON_PIPE.join(publisher.name(), DATE_FORMAT.print(now)))
                .build();
    }

    @Override
    public void updateFeedStatistics(Publisher publisher, Duration latency) {
        try {
            latencyLock.tryLock();

            DBObject today = collection.findOne(idQuery(publisher, clock.now()));

            if (today == null) {
                today = idQuery(publisher, clock.now());
                
                TranslatorUtils.fromDuration(today, LATENCY_SUM_KEY, latency);
                TranslatorUtils.from(today, LATENCY_COUNT_KEY, 1);
            } else {
                Duration totalLatency = TranslatorUtils.toDuration(today, LATENCY_SUM_KEY);
                int totalCount = TranslatorUtils.toInteger(today, LATENCY_COUNT_KEY);
                
                TranslatorUtils.fromDuration(today, LATENCY_SUM_KEY, totalLatency.plus(latency));
                TranslatorUtils.from(today, LATENCY_COUNT_KEY, totalCount + 1);
            }


            collection.save(today);
        } finally {
            latencyLock.unlock();
        }
    }
}
