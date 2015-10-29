package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.joda.time.LocalDate;

public class MongoSentBroadcastEventPcridStore implements SentBroadcastEventPcridStore {

    private static final Joiner KEY_JOINER = Joiner.on("|").skipNulls();
    
    private static final String COLLECTION = "sentBroadcastProgramUrls";

    private static final String BROADCAST_EVENT_IMI_KEY = "broadcastEventImi";
        
    private final DBCollection collection;
    
    public MongoSentBroadcastEventPcridStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION);
    }
    
    @Override
    public void recordSent(String broadcastEventImi, LocalDate broadcastTransmissionDate, String itemCrid, String programmeCrid) {
        if (find(itemCrid, programmeCrid) != null) {
            return;
        }

        BroadcastEventRecord eventRecords = new BroadcastEventRecord.Builder()
                                                            .broadcastEventImi(broadcastEventImi)
                                                            .broadcastTransmissionDate(broadcastTransmissionDate)
                                                            .build();
        collection.save(BasicDBObjectBuilder
                .start(MongoConstants.ID, keyFrom(itemCrid, programmeCrid))
                .add(BROADCAST_EVENT_IMI_KEY, eventRecords)
                .get());
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
        return Optional.of(toBroadcastEventRecords(found, BROADCAST_EVENT_IMI_KEY));
    }

    public static BroadcastEventRecord toBroadcastEventRecords(DBObject dbObject, String name){
        if(dbObject.containsField(name)){
            return (BroadcastEventRecord) dbObject.get(name);
        }
        return null;
    }

    private DBObject find(String itemCrid, String programmeCrid) {
        return collection.findOne(keyFrom(itemCrid, programmeCrid));
    }
    
    private String keyFrom(String itemCrid, String programmeCrid) {
        return KEY_JOINER.join(itemCrid, programmeCrid);
    }
    
}
