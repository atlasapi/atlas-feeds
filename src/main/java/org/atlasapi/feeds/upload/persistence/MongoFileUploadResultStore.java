package org.atlasapi.feeds.upload.persistence;

import static com.metabroadcast.common.persistence.mongo.MongoBuilders.sort;
import static com.metabroadcast.common.persistence.mongo.MongoBuilders.where;
import static com.metabroadcast.common.persistence.mongo.MongoConstants.SINGLE;
import static com.metabroadcast.common.persistence.mongo.MongoConstants.UPSERT;
import static org.atlasapi.feeds.upload.persistence.FileUploadResultTranslator.SERVICE_KEY;
import static org.atlasapi.feeds.upload.persistence.FileUploadResultTranslator.TIME_KEY;
import static org.atlasapi.feeds.upload.persistence.FileUploadResultTranslator.FILENAME;

import java.util.regex.Pattern;

import org.atlasapi.feeds.upload.FileUploadResult;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoFileUploadResultStore implements FileUploadResultStore {
    
    private DBCollection collection;
    private FileUploadResultTranslator translator;

    public MongoFileUploadResultStore(DatabasedMongo mongo) {
        this.collection = mongo.collection("uploads");
        this.translator = new FileUploadResultTranslator();
    }
    
    @Override
    public void store(String identifier, FileUploadResult result) {
        
        DBObject dbo = translator.toDBObject(result);
        String oid = String.format("%s:%s:%s", result.remote(), result.type(), identifier);
        dbo.put(MongoConstants.ID, oid);
        dbo.put("id", identifier);
        collection.update(new BasicDBObject(MongoConstants.ID, oid), dbo, UPSERT, SINGLE);
    }

    @Override
    public Iterable<FileUploadResult> result(String service, String identifierPrefix) {
        return transform(where().fieldEquals(SERVICE_KEY, service).fieldEquals("id", Pattern.compile("^"+identifierPrefix)).build());
    }

    private Iterable<FileUploadResult> transform(DBObject query) {
        return Iterables.transform(collection.find(query).sort(sort().descending(TIME_KEY).build()), new Function<DBObject, FileUploadResult>() {
            @Override
            public FileUploadResult apply(DBObject input) {
                return translator.fromDBObject(input);
            }
        });
    }

    @Override
    public Iterable<FileUploadResult> results(String service) {
        return transform(where().fieldEquals(SERVICE_KEY, service).build());
    }

    @Override
    public Maybe<FileUploadResult> latestResultFor(String service,
            String fileName) {
        return Maybe.fromPossibleNullValue(Iterables.getOnlyElement(Iterables.limit(
                transform(where().fieldEquals(SERVICE_KEY, service).fieldEquals(FILENAME, fileName).build()), 1), null));
    }

}
