package org.atlasapi.feeds.youview.statistics;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TaskQuery;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;

import com.mongodb.QueryBuilder;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.common.time.Clock;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MongoFeedStatisticsStore implements FeedStatisticsResolver {

    private static final String COLLECTION_NAME = "youviewTasks";

    private final DBCollection collection;
    private final TaskStore taskStore;
    private final Clock clock;
    private final DestinationType destinationType;

    public MongoFeedStatisticsStore(DatabasedMongo mongo, TaskStore taskStore, 
            Clock clock, DestinationType destinationType) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
        this.taskStore = checkNotNull(taskStore);
        this.clock = checkNotNull(clock);
        this.destinationType = checkNotNull(destinationType);
    }

    @Override
    public Optional<FeedStatistics> resolveFor(Publisher publisher) {
        int createdTasks = getTasksCreatedInTheLastFourHours();
        int failedTasks = getTasksFailedInTheLastFourHours();

        Optional<Duration> latency = calculateLatency(publisher);
        if (!latency.isPresent()) {
            return Optional.of(new FeedStatistics(publisher, 0, Duration.ZERO, createdTasks, failedTasks));
        }

        int queueSize = calculateCurrentQueueSize(publisher);
        
        return Optional.of(new FeedStatistics(publisher, queueSize, latency.get(), createdTasks, failedTasks));
    }

    private int calculateCurrentQueueSize(Publisher publisher) {
        return Iterables.size(taskStore.allTasks(
                TaskQuery.builder(Selection.all(), publisher, destinationType)
                        .withTaskStatus(Status.NEW)
                        .build()));
    }

    private Optional<Duration> calculateLatency(Publisher publisher) {
        BasicDBObject query = new BasicDBObject();
        query.put("status", Status.NEW.name());
        query.put("publisher", publisher.key());
        query.put("destinationType", destinationType.name());

        DBObject stats = collection
                .find(query)
                .sort(new BasicDBObject("created", 1))
                .limit(1)
                .one();

        if (stats == null) {
            return Optional.absent();
        }

        DateTime oldestMessage = TranslatorUtils.toDateTime(stats, "created");

        return Optional.of(new Duration(oldestMessage, clock.now()));
    }

    private int getTasksCreatedInTheLastFourHours() {
        Date fourHoursAgo = DateTime.now().minusHours(4).toDate();

        DBObject publishedClause = QueryBuilder.start("status").is("PUBLISHED").get();
        DBObject acceptedClause = QueryBuilder.start("status").is("ACCEPTED").get();

        DBObject query = QueryBuilder.start()
                .or(publishedClause, acceptedClause)
                .and("created").greaterThanEquals(fourHoursAgo)
                .get();

        return collection.find(query).count();
    }

    private int getTasksFailedInTheLastFourHours() {
        Date fourHoursAgo = DateTime.now().minusHours(4).toDate();

        DBObject query = QueryBuilder.start()
                .put("created").greaterThanEquals(fourHoursAgo)
                .and("status").is("REJECTED")
                .get();

        return collection.find(query).count();
    }
}
