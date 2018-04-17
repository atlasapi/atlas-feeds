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
import org.joda.time.Period;

public class MongoFeedStatisticsStore implements FeedStatisticsResolver {

    private static final String COLLECTION_NAME = "youviewTasks";
    private static final String STATUS_KEY = "status";
    private static final String CREATED_KEY = "created";

    private final DBCollection collection;
    private final TaskStore taskStore;
    private final Clock clock;
    private final DestinationType destinationType;

    private MongoFeedStatisticsStore(Builder builder) {
        this.collection = checkNotNull(builder.mongo).collection(COLLECTION_NAME);
        this.taskStore = checkNotNull(builder.taskStore);
        this.clock = checkNotNull(builder.clock);
        this.destinationType = checkNotNull(builder.destinationType);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<FeedStatistics> resolveFor(Publisher publisher, Period timeBeforeNow) {
        int successfulTasks = getTasksCreatedInTheLastDurationByStatus(
                timeBeforeNow,
                Status.ACCEPTED.name(),
                Status.PUBLISHED.name()
        );
        int unsuccessfulTasks = getTasksCreatedInTheLastDurationByStatus(
                timeBeforeNow,
                Status.FAILED.name(),
                Status.REJECTED.name()
        );

        Optional<Duration> latency = calculateLatency(publisher);
        if (!latency.isPresent()) {
            return Optional.of(FeedStatistics.build()
                    .withPublisher(publisher)
                    .withQueueSize(0)
                    .withUpdateLatency(Duration.ZERO)
                    .withSuccessfulTasks(successfulTasks)
                    .withUnsuccessfulTasks(unsuccessfulTasks)
                    .build());
        }

        int queueSize = calculateCurrentQueueSize(publisher);
        
        return Optional.of(FeedStatistics.build()
                .withPublisher(publisher)
                .withQueueSize(queueSize)
                .withUpdateLatency(latency.get())
                .withSuccessfulTasks(successfulTasks)
                .withUnsuccessfulTasks(unsuccessfulTasks)
                .build());
    }

    private int calculateCurrentQueueSize(Publisher publisher) {
        return Iterables.size(taskStore.allTasks(
                TaskQuery.builder(Selection.all(), destinationType)
                        .withPublisher(publisher)
                        .withTaskStatus(Status.NEW)
                        .withSort(TaskQuery.Sort.DEFAULT)
                        .build()));
    }

    private Optional<Duration> calculateLatency(Publisher publisher) {
        BasicDBObject query = new BasicDBObject();
        query.put(STATUS_KEY, Status.NEW.name());
        query.put("publisher", publisher.key());
        query.put("destinationType", destinationType.name());

        DBObject stats = collection
                .find(query)
                .sort(new BasicDBObject(CREATED_KEY, 1))
                .limit(1)
                .one();

        if (stats == null) {
            return Optional.absent();
        }

        DateTime oldestMessage = TranslatorUtils.toDateTime(stats, CREATED_KEY);

        return Optional.of(new Duration(oldestMessage, clock.now()));
    }

    private int getTasksCreatedInTheLastDurationByStatus(
            Period timeBeforeNow,
            String firstStatus,
            String secondStatus
    ) {
        DBObject firstStatusClause = QueryBuilder.start(STATUS_KEY).is(firstStatus).get();
        DBObject secondStatusClause = QueryBuilder.start(STATUS_KEY).is(secondStatus).get();

        Date timeBeforePeriod = new DateTime().minus(timeBeforeNow).toDate();
        DBObject query = QueryBuilder.start(CREATED_KEY).greaterThanEquals(timeBeforePeriod)
                .or(firstStatusClause, secondStatusClause)
                .get();

        return collection.find(query).count();
    }

    public static class Builder {

        private DatabasedMongo mongo;
        private TaskStore taskStore;
        private Clock clock;
        private Destination.DestinationType destinationType;

        private Builder() {}

        public Builder withMongoDatabase(DatabasedMongo mongo) {
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
            return new MongoFeedStatisticsStore(this);
        }
    }
}
