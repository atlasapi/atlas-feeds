package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoIdMappingStore implements IdMappingStore {

    private static final String COLLECTION = "youViewIdMappings";
    private static final String VALUE = "value";

    private final DBCollection collection;

    public MongoIdMappingStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION);
    }

    @Override
    public void storeMapping(String key, String value) {
        collection.save(BasicDBObjectBuilder
                .start(MongoConstants.ID, key)
                .add(VALUE, value)
                .get());
    }

    @Override
    public Optional<String> getValueFor(String key) {
        DBObject found = collection.findOne(new BasicDBObject(MongoConstants.ID, key));

        if (found == null) {
            return Optional.absent();
        }

        return Optional.of(TranslatorUtils.toString(found, VALUE));
    }
}
