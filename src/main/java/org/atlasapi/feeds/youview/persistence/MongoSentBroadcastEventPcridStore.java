package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.joda.time.LocalDate;

public class MongoSentBroadcastEventPcridStore implements SentBroadcastEventPcridStore {

    private static final Joiner KEY_JOINER = Joiner.on("|").skipNulls();
    
    private static final String COLLECTION = "sentBroadcastProgramUrls";

    private static final String BROADCAST_EVENT_IMI_KEY = "broadcastEventImi";

    //We are using this to record the time we last sent
    private static final String BROADCAST_EVENT_TRANSMISSION_TIME = "transmissionTime";
        
    private final DBCollection collection;
    
    public MongoSentBroadcastEventPcridStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION);
    }
    
    @Override
    public void recordSent(String broadcasteEventImi, LocalDate broadcastTransmissionTime, String itemCrid, String programmeCrid) {
        if (find(itemCrid, programmeCrid) != null) {
            return;
        }
        collection.save(BasicDBObjectBuilder
                .start(MongoConstants.ID, keyFrom(itemCrid, programmeCrid))
                .add(BROADCAST_EVENT_IMI_KEY, broadcasteEventImi)
                .add(BROADCAST_EVENT_TRANSMISSION_TIME, broadcastTransmissionTime)
                .get());
    }
    
    @Override
    public void removeSentRecord(String crid, String programUrl) {
        collection.remove(new MongoQueryBuilder()
                .idEquals(keyFrom(crid, programUrl))
                .build());
    }
    
    @Override
    public Optional<String> getSentBroadcastEventImi(String itemCrid, String programmeCrid) {
        DBObject found = find(itemCrid, programmeCrid);
        if (found == null) {
            return Optional.absent();
        }
        return Optional.of(TranslatorUtils.toString(found,  BROADCAST_EVENT_IMI_KEY));
    }

    @Override
    public Optional<LocalDate> getSentBroadcastEventTransmissionDate(String itemCrid, String programmeCrid) {
        DBObject found = find(itemCrid, programmeCrid);
        if(found == null){
            return Optional.absent();
        }
        return Optional.of(toDate(found, BROADCAST_EVENT_TRANSMISSION_TIME));
    }

    //commom.persistence.translator.TranslatorUtils does not have toDate yet so this can be moved there.
    public static LocalDate toDate(DBObject object, String name) {
        if(object.containsField(name)) {
            Object result = object.get(name);
            if(result instanceof LocalDate) {
                return (LocalDate)object.get(name);
            }
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
