package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Joiner;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MongoSentBroadcastEventProgramUrlStore implements SentBroadcastEventProgramUrlStore {

    private static final Joiner KEY_JOINER = Joiner.on(":").skipNulls();
    
    private static final String COLLECTION = "sentBroadcastProgramUrls";
    
    private final DBCollection collection;
    
    public MongoSentBroadcastEventProgramUrlStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION);
    }
    
    @Override
    public void recordSent(String crid, String programUrl, String serviceIdRef) {
        if (find(crid, programUrl, serviceIdRef) != null) {
            return;
        }
        collection.save(BasicDBObjectBuilder
                            .start(MongoConstants.ID, keyFrom(crid, programUrl, serviceIdRef))
                            .get());
    }
    
    @Override
    public void removeSentRecord(String crid, String programUrl, String serviceIdRef) {
        collection.remove(new MongoQueryBuilder()
                                .idEquals(keyFrom(crid, programUrl, serviceIdRef))
                                .build());
    }
    
    @Override
    public boolean beenSent(String crid, String programUrl, String serviceIdRef) {
        return find(crid, programUrl, serviceIdRef) != null;
    }
    
    private DBObject find(String crid, String programUrl, String serviceIdRef) {
        return collection.findOne(keyFrom(crid, programUrl, serviceIdRef));
    }
    
    private String keyFrom(String crid, String programUrl, String serviceIdRef) {
        return KEY_JOINER.join(crid, programUrl, serviceIdRef);
    }
    
}
