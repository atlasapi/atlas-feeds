package org.atlasapi.feeds.youview.statistics;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.tasks.Destination;
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

    private MongoFeedStatisticsStore(
            DatabasedMongo mongo,
            TaskStore taskStore,
            Clock clock,
            DestinationType destinationType
    ) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
        this.taskStore = checkNotNull(taskStore);
        this.clock = checkNotNull(clock);
        this.destinationType = checkNotNull(destinationType);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<FeedStatistics> resolveFor(Publisher publisher, Duration duration) {
        int successfulTasks = getTasksInTheLastFourHours(duration, "ACCEPTED", "PUBLISHED");
        int unsuccessfulTasks = getTasksInTheLastFourHours(duration, "FAILED", "REJECTED");

        Optional<Duration> latency = calculateLatency(publisher);
        if (!latency.isPresent()) {
            return Optional.of(FeedStatistics.build()
                    .withPublisher(publisher)
                    .withQueueSize(0)
                    .withUpdateLatency(Duration.ZERO)
                    .withSuccessfulTasks(successfulTasks)
                    .withUnsuccessfulTasks(unsuccessfulTasks)
                    .createFeedStatistics());
        }

        int queueSize = calculateCurrentQueueSize(publisher);
        
        return Optional.of(FeedStatistics.build()
                .withPublisher(publisher)
                .withQueueSize(queueSize)
                .withUpdateLatency(latency.get())
                .withSuccessfulTasks(successfulTasks)
                .withUnsuccessfulTasks(unsuccessfulTasks)
                .createFeedStatistics());
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

    private int getTasksInTheLastFourHours(Duration duration, String taskStatus1, String taskStatus2) {
        DBObject firstClause = QueryBuilder.start("status").is(taskStatus1).get();
        DBObject secondClause = QueryBuilder.start("status").is(taskStatus2).get();

        Date durationTimeAgo = new DateTime().minus(duration).toDate();
        DBObject query = QueryBuilder.start()
                .or(firstClause, secondClause)
                .and("created").greaterThanEquals(durationTimeAgo)
                .get();

        return collection.find(query).count();
    }

    public static class Builder {

        private DatabasedMongo mongo;
        private TaskStore taskStore;
        private Clock clock;
        private Destination.DestinationType destinationType;

        private Builder() {}

        public Builder withMongoCollection(DatabasedMongo mongo) {
            this.mongo = mongo;
            return this;
        }

        public Builder withTaskStore(TaskStore taskStore) {
            this.taskStore = taskStore;
            return this;
        }

        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withDestinationType(
                Destination.DestinationType destinationType) {
            this.destinationType = destinationType;
            return this;
        }

        public MongoFeedStatisticsStore build() {
            return new MongoFeedStatisticsStore(mongo, taskStore, clock, destinationType);
        }
    }
}
