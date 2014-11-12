package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.management.ManagementFactory;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.metabroadcast.common.time.Clock;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public final class FeedStatisticsTranslator {
    
    static final String QUEUE_SIZE_KEY = "queueSize";
    static final String UPDATE_LATENCY_KEY = "updateLatency";
    private final Clock clock;

    public FeedStatisticsTranslator(Clock clock) {
        this.clock = checkNotNull(clock);
    }

    public DBObject toDBObject(FeedStatistics feedStatistics) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, feedStatistics.publisher().name());
        TranslatorUtils.from(dbo, QUEUE_SIZE_KEY, feedStatistics.queueSize());
        TranslatorUtils.fromDuration(dbo, UPDATE_LATENCY_KEY, feedStatistics.updateLatency());
        
        return dbo;
    }
    
    public FeedStatistics fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        
        Publisher publisher = Publisher.valueOf(TranslatorUtils.toString(dbo, MongoConstants.ID));
        int queueSize = TranslatorUtils.toInteger(dbo, QUEUE_SIZE_KEY);
        Duration updateLatency = TranslatorUtils.toDuration(dbo, UPDATE_LATENCY_KEY);
        Duration jvmUptime = Duration.millis(ManagementFactory.getRuntimeMXBean().getUptime());
        DateTime lastOutage = clock.now().minus(jvmUptime);
        
        return new FeedStatistics(publisher, queueSize, updateLatency, lastOutage);
    }
}
