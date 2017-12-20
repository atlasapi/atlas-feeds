package org.atlasapi.feeds.youview.persistence;

import java.util.Optional;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoYouViewPayloadHashStore implements YouViewPayloadHashStore {

    private static final String COLLECTION_NAME = "youViewPayloadHashes";
    private static final String HASH_FIELD = "hash";

    private final DBCollection collection;

    public MongoYouViewPayloadHashStore(DatabasedMongo mongo) {
        this.collection = mongo.collection(COLLECTION_NAME);
    }

    @Override
    public void saveHash(HashType payloadType, String imi, String hash) {
        DBObject dbObject = BasicDBObjectBuilder
                .start(MongoConstants.ID, key(payloadType, imi))
                .add(HASH_FIELD, hash)
                .get();
        collection.save(dbObject);
    }

    @Override
    public Optional<String> getHash(HashType hashType, String id) {
        DBObject dbObject = collection.findOne(key(hashType, id));

        if (dbObject != null) {
            return Optional.ofNullable(TranslatorUtils.toString(dbObject, HASH_FIELD));
        } else {
            return Optional.empty();
        }
    }

    private String key(HashType payloadType, String imi) {
        return payloadType.name() + "-" + imi;
    }
}
