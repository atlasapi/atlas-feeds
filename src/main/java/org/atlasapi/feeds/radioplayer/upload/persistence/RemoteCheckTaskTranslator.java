package org.atlasapi.feeds.radioplayer.upload.persistence;

import static org.atlasapi.feeds.radioplayer.upload.queue.TranslationUtils.toMap;

import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.MongoTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckTask;

import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class RemoteCheckTaskTranslator implements MongoTranslator<RemoteCheckTask> {

    private static final String FILE_KEY = "file";
    private static final String UPLOAD_DETAILS_KEY = "uploadDetails";
    private static final String TIMESTAMP_KEY = "timestamp";

    private final MongoTranslator<RadioPlayerFile> fileTranslator = new RadioPlayerFileTranslator();
    
    @Override
    public DBObject toDBObject(RemoteCheckTask task) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, task.file().toKey());
        TranslatorUtils.from(dbo, FILE_KEY, task.file());
        TranslatorUtils.fromDateTime(dbo, TIMESTAMP_KEY, task.timestamp());
        TranslatorUtils.from(dbo, UPLOAD_DETAILS_KEY, task.uploadDetails());
        
        return dbo;
    }

    @Override
    public RemoteCheckTask fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        return new RemoteCheckTask(
                fileTranslator.fromDBObject(TranslatorUtils.toDBObject(dbo, FILE_KEY)),
                TranslatorUtils.toDateTime(dbo, TIMESTAMP_KEY),
                toMap(dbo, UPLOAD_DETAILS_KEY)
        );
    }
}
