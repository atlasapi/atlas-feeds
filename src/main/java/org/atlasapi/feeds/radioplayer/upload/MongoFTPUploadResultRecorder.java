package org.atlasapi.feeds.radioplayer.upload;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.mongodb.DBCollection;

public class MongoFTPUploadResultRecorder implements FTPUploadResultRecorder {

    private DBCollection recordings;
    private FTPUploadResultTranslator translator;

    public MongoFTPUploadResultRecorder(DatabasedMongo mongo) {
        this.recordings = mongo.collection("radioplayer");
        this.translator = new FTPUploadResultTranslator();
    }
    
    @Override
    public void record(FTPUploadResult result) {
        this.recordings.insert(translator.toDBObject(result));
    }

}
