package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.persistence.mongo.MongoConstants.ID;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoFTPUploadResultRecorder implements FTPUploadResultRecorder {

    private DBCollection recordings;
    private FTPUploadResultTranslator translator;

    public MongoFTPUploadResultRecorder(DatabasedMongo mongo) {
        this.recordings = mongo.collection("radioplayer");
        this.translator = new FTPUploadResultTranslator();
    }
    
    @Override
    public void record(FTPUploadResult result) {
            this.recordings.update(query(result), translator.toDBObject(result), true, false);
    }

    private DBObject query(FTPUploadResult result) {
        return new BasicDBObject(ID, result.type()+":"+result.filename());
    }

}
