package org.atlasapi.feeds.youview.revocation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public final class MongoRevokedContentStore implements RevokedContentStore {

    private static final String COLLECTION_NAME = "youviewRevokedContent";
    
    private final DBCollection collection;
    
    public MongoRevokedContentStore(DatabasedMongo mongo) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
    }
    
    @Override
    public void revoke(String uri) {
        DBObject dbo = new MongoQueryBuilder().idEquals(uri).build();
        collection.save(dbo);
    }

    @Override
    public boolean unrevoke(String uri) {
        DBObject idQuery = new MongoQueryBuilder().idEquals(uri).build();
        
        DBObject resolved = collection.findOne(idQuery);
        collection.remove(idQuery);
        
        return resolved != null;
    }

    @Override
    public boolean isRevoked(String uri) {
        DBObject idQuery = new MongoQueryBuilder().idEquals(uri).build();
        return collection.findOne(idQuery) != null;
    }

    @Override
    public void clearAllRevocations() {
        collection.remove(new BasicDBObject());
    }

}
