package org.atlasapi.feeds.radioplayer.upload;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.mongodb.DBCollection;

public class MongoRadioPlayerUploadResultRecorder implements RadioPlayerUploadResultRecorder {

    private DBCollection recordings;
    private RadioPlayerUploadResultTranslator translator;

    public MongoRadioPlayerUploadResultRecorder(DatabasedMongo mongo) {
        this.recordings = mongo.collection("radioplayer");
        this.translator = new RadioPlayerUploadResultTranslator();
    }
    
    @Override
    public void record(Iterable<RadioPlayerUploadResult> results) {
        for (RadioPlayerUploadResult result : results) {
            this.recordings.insert(translator.toDBObject(result));
        }
    }

}
