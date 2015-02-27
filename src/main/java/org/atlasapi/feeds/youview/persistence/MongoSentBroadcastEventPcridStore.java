package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Joiner;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MongoSentBroadcastEventPcridStore implements SentBroadcastEventPcridStore {

    private static final Joiner KEY_JOINER = Joiner.on("|").skipNulls();
    
    private static final String COLLECTION = "sentBroadcastProgramUrls";
    
    private final DBCollection collection;
    
    public MongoSentBroadcastEventPcridStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION);
    }
    
    @Override
    public void recordSent(String crid, String programUrl) {
        if (find(crid, programUrl) != null) {
            return;
        }
        collection.save(BasicDBObjectBuilder
                            .start(MongoConstants.ID, keyFrom(crid, programUrl))
                            .get());
    }
    
    @Override
    public void removeSentRecord(String crid, String programUrl) {
        collection.remove(new MongoQueryBuilder()
                                .idEquals(keyFrom(crid, programUrl))
                                .build());
    }
    
    @Override
    public boolean beenSent(String crid, String programUrl) {
        return find(crid, programUrl) != null;
    }
    
    private DBObject find(String crid, String programUrl) {
        return collection.findOne(keyFrom(crid, programUrl));
    }
    
    private String keyFrom(String crid, String programUrl) {
        return KEY_JOINER.join(crid, programUrl);
    }
    
}
