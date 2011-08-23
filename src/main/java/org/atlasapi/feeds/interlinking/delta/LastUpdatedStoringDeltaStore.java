package org.atlasapi.feeds.interlinking.delta;

import static com.metabroadcast.common.persistence.mongo.MongoBuilders.update;
import static com.metabroadcast.common.persistence.mongo.MongoConstants.ID;
import static com.metabroadcast.common.persistence.mongo.MongoConstants.SINGLE;
import static com.metabroadcast.common.persistence.mongo.MongoConstants.UPSERT;
import nu.xom.Document;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class LastUpdatedStoringDeltaStore implements InterlinkingDeltaStore {

    private static final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd");
    private static final String UPDATED = "updated";
    private final InterlinkingDocumentStore documentStore;
    private final DBCollection collection;

    public LastUpdatedStoringDeltaStore(InterlinkingDocumentStore documentStore, DatabasedMongo mongo) {
        this.documentStore = documentStore;
        this.collection = mongo.collection("interlinking");
    }
    
    @Override
    public void storeDelta(DateTime day, InterlinkingDelta delta) {
        documentStore.storeDocument(key(day), delta.document());
        store(day, delta.lastUpdated());
    }

    @Override
    public InterlinkingDelta getExistingDelta(DateTime day) {
        Maybe<Document> document;
        try {
            document = documentStore.getDocument(key(day));
        } catch (Exception e){
            document = Maybe.nothing();
        }
        return InterlinkingDelta.deltaFor(document, lastUpdatedFor(day));
    }
    
    private DateTime lastUpdatedFor(DateTime day) {
        DBObject lastUpdated = collection.findOne(key(day));
        if(lastUpdated == null ){
            return null;
        }
        return TranslatorUtils.toDateTime(lastUpdated, UPDATED);
    }

    private void store(DateTime day, DateTime lastUpdated) {
        collection.update(new BasicDBObject(ID, key(day)), update().setField(UPDATED, lastUpdated).build(), UPSERT, SINGLE);
    }
    
    private String key(DateTime day) {
        return dateTimeFormat.print(day);
    }

}
