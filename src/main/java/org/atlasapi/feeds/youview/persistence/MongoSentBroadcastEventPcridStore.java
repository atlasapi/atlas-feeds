package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.joda.time.LocalDate;

public class MongoSentBroadcastEventPcridStore implements SentBroadcastEventPcridStore {

    private static final Joiner KEY_JOINER = Joiner.on("|").skipNulls();
    
    private static final String COLLECTION = "sentBroadcastProgramUrls";

    private static final String BROADCAST_EVENT_IMI_KEY = "broadcastEventImi";

    private static final String BROADCAST_EVENT_TRANSMISSION_DATE = "broadcastTransmissionDate";
        
    private final DBCollection collection;
    
    public MongoSentBroadcastEventPcridStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION);
    }
    
    @Override
    public void recordSent(String broadcastEventImi, LocalDate broadcastTransmissionDate, String itemCrid, String programmeCrid) {
        if (find(itemCrid, programmeCrid) != null) {
            return;
        }

        BasicDBObject dbo = new BasicDBObject();
        dbo.put(MongoConstants.ID, keyFrom(itemCrid, programmeCrid));
        TranslatorUtils.fromLocalDate(dbo, BROADCAST_EVENT_TRANSMISSION_DATE, broadcastTransmissionDate);
        TranslatorUtils.from(dbo, BROADCAST_EVENT_IMI_KEY, broadcastEventImi);

        collection.save(dbo);
    }
    
    @Override
    public void removeSentRecord(String crid, String programUrl) {
        collection.remove(new MongoQueryBuilder()
                                .idEquals(keyFrom(crid, programUrl))
                                .build());
    }
    
    @Override
    public Optional<BroadcastEventRecord> getSentBroadcastEventRecords(String itemCrid, String programmeCrid) {
        DBObject found = find(itemCrid, programmeCrid);
        if (found == null) {
            return Optional.absent();
        }
        return Optional.of(toBroadcastEventRecords(found));
    }

    private BroadcastEventRecord toBroadcastEventRecords(DBObject dbObject) {
        LocalDate broadcastTransmissionDate =TranslatorUtils.toLocalDate(dbObject, BROADCAST_EVENT_TRANSMISSION_DATE);
        String broadcastEventImi = TranslatorUtils.toString(dbObject, BROADCAST_EVENT_IMI_KEY);

        BroadcastEventRecord eventRecords = new BroadcastEventRecord.Builder()
                .broadcastEventImi(broadcastEventImi)
                .broadcastTransmissionDate(broadcastTransmissionDate)
                .build();

        return eventRecords;
    }

    private DBObject find(String itemCrid, String programmeCrid) {
        return collection.findOne(keyFrom(itemCrid, programmeCrid));
    }
    
    private String keyFrom(String itemCrid, String programmeCrid) {
        return KEY_JOINER.join(itemCrid, programmeCrid);
    }
    
}
