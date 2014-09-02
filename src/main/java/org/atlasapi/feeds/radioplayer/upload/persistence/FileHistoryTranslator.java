package org.atlasapi.feeds.radioplayer.upload.persistence;

import java.util.List;

import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.MongoTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class FileHistoryTranslator implements MongoTranslator<FileHistory> {
    
    private static final String FILE_KEY = "file";
    private static final String UPLOAD_ATTEMPTS_KEY = "uploadAttempts";
    
    private final MongoTranslator<RadioPlayerFile> fileTranslator = new RadioPlayerFileTranslator();
    private final MongoTranslator<UploadAttempt> attemptTranslator = new UploadAttemptTranslator(); 

    public DBObject toDBObject(FileHistory file) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, file.file().toKey());
        TranslatorUtils.from(dbo, FILE_KEY, fileTranslator.toDBObject(file.file()));
        TranslatorUtils.from(dbo, UPLOAD_ATTEMPTS_KEY, attemptsToDBObject(file));
        
        return dbo;
    }

    public FileHistory fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        
        return new FileHistory(
                fileTranslator.fromDBObject(TranslatorUtils.toDBObject(dbo, FILE_KEY)),
                attemptsFromDBObject(TranslatorUtils.toDBObjectList(dbo, UPLOAD_ATTEMPTS_KEY))
        );
    }

    private BasicDBList attemptsToDBObject(FileHistory file) {
        BasicDBList values = new BasicDBList();
        for (UploadAttempt attempt : file.uploadAttempts()) {
            if (attempt != null) {
                values.add(attemptTranslator.toDBObject(attempt));
            }
        }
        return values;
    }

    private Iterable<UploadAttempt> attemptsFromDBObject(List<DBObject> dbos) {
        return Iterables.transform(dbos, new Function<DBObject, UploadAttempt>() {
                    @Override
                    public UploadAttempt apply(DBObject input) {
                        return attemptTranslator.fromDBObject(input);
                    }
                }
        );
    }
}
