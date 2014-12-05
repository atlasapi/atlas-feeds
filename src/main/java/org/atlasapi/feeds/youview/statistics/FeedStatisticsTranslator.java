package org.atlasapi.feeds.youview.statistics;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;

import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public final class FeedStatisticsTranslator {
    
    static final String QUEUE_SIZE_KEY = "queueSize";
    static final String UPDATE_LATENCY_KEY = "updateLatency";

    private FeedStatisticsTranslator() {
        // private constructor for factory class
    }

    public static DBObject toDBObject(FeedStatistics feedStatistics) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, feedStatistics.publisher().name());
        TranslatorUtils.from(dbo, QUEUE_SIZE_KEY, feedStatistics.queueSize());
        TranslatorUtils.fromDuration(dbo, UPDATE_LATENCY_KEY, feedStatistics.updateLatency());
        
        return dbo;
    }
    
    public static FeedStatistics fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        
        Publisher publisher = Publisher.valueOf(TranslatorUtils.toString(dbo, MongoConstants.ID));
        int queueSize = TranslatorUtils.toInteger(dbo, QUEUE_SIZE_KEY);
        Duration updateLatency = TranslatorUtils.toDuration(dbo, UPDATE_LATENCY_KEY);
        
        return new FeedStatistics(publisher, queueSize, updateLatency);
    }
}
