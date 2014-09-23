package org.atlasapi.feeds.radioplayer.upload.persistence;

import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.MongoTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;

import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class UploadTaskTranslator implements MongoTranslator<UploadTask> {

    private static final String FILE_KEY = "file";
    static final String TIMESTAMP_KEY = "timestamp";

    private final MongoTranslator<RadioPlayerFile> fileTranslator = new RadioPlayerFileTranslator();
    
    @Override
    public DBObject toDBObject(UploadTask task) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, task.file().toKey());
        TranslatorUtils.from(dbo, FILE_KEY, fileTranslator.toDBObject(task.file()));
        TranslatorUtils.fromDateTime(dbo, TIMESTAMP_KEY, task.timestamp());
        
        return dbo;
    }

    @Override
    public UploadTask fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        
        return new UploadTask(
                fileTranslator.fromDBObject(TranslatorUtils.toDBObject(dbo, FILE_KEY)),
                TranslatorUtils.toDateTime(dbo, TIMESTAMP_KEY)
        );
    }
}
