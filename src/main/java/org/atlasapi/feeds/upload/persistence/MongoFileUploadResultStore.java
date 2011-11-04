package org.atlasapi.feeds.upload.persistence;

import static com.metabroadcast.common.persistence.mongo.MongoConstants.SINGLE;
import static com.metabroadcast.common.persistence.mongo.MongoConstants.UPSERT;

import java.util.regex.Pattern;

import org.atlasapi.feeds.upload.FileUploadResult;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
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
    public void store(String service, String identifier, FileUploadResult result) {
        DBObject dbo = translator.toDBObject(result);
        String oid = String.format("%s:%s:%s", service, result.type(), identifier);
        dbo.put(MongoConstants.ID, oid);
        dbo.put("service", service);
        dbo.put("id", identifier);
        collection.update(new BasicDBObject(MongoConstants.ID, oid), dbo, UPSERT, SINGLE);
    }

    @Override
    public Iterable<FileUploadResult> result(String service, String identifierPrefix) {
        return transform(new BasicDBObjectBuilder().add("service", service).add("id", Pattern.compile("^"+identifierPrefix)).get());
    }

    private Iterable<FileUploadResult> transform(DBObject query) {
        return Iterables.transform(collection.find(query).sort(new BasicDBObject("time", -1)), new Function<DBObject, FileUploadResult>() {
            @Override
            public FileUploadResult apply(DBObject input) {
                return translator.fromDBObject(input);
            }
        });
    }

    @Override
    public Iterable<FileUploadResult> results(String service) {
        return transform(new BasicDBObjectBuilder().add("service", service).get());
    }

}
