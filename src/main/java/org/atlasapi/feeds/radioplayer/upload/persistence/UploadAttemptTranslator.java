package org.atlasapi.feeds.radioplayer.upload.persistence;

import static org.atlasapi.feeds.radioplayer.upload.queue.TranslationUtils.toMap;

import org.atlasapi.feeds.radioplayer.upload.queue.MongoTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;

import com.google.common.base.Preconditions;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class UploadAttemptTranslator implements MongoTranslator<UploadAttempt> {

    private static final String ID_KEY = "id";
    private static final String UPLOAD_TIME_KEY = "uploadTime";
    private static final String UPLOAD_RESULT_KEY = "uploadResult";
    private static final String UPLOAD_DETAILS_KEY = "uploadDetails";
    private static final String REMOTE_CHECK_RESULT_KEY = "remoteCheckResult";
    private static final String REMOTE_CHECK_MESSAGE_KEY = "remoteCheckMessage";

    public DBObject toDBObject(UploadAttempt attempt) {
        Preconditions.checkArgument(attempt.id() != null, "id on upload must be set before writing");

        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, ID_KEY, attempt.id());
        TranslatorUtils.fromDateTime(dbo, UPLOAD_TIME_KEY, attempt.uploadTime());
        TranslatorUtils.from(dbo, UPLOAD_RESULT_KEY, attempt.uploadResult().name());
        TranslatorUtils.from(dbo, UPLOAD_DETAILS_KEY, attempt.uploadDetails());
        if (attempt.remoteCheckResult() != null) {
            TranslatorUtils.from(dbo, REMOTE_CHECK_RESULT_KEY, attempt.remoteCheckResult().name());
        }
        TranslatorUtils.from(dbo, REMOTE_CHECK_MESSAGE_KEY, attempt.remoteCheckMessage());
        
        return dbo;
    }

    public UploadAttempt fromDBObject(DBObject dbo) {
        return new UploadAttempt(
                TranslatorUtils.toLong(dbo, ID_KEY),
                TranslatorUtils.toDateTime(dbo, UPLOAD_TIME_KEY), 
                translateUploadResult(dbo, UPLOAD_RESULT_KEY),
                toMap(dbo, UPLOAD_DETAILS_KEY),
                translateUploadResult(dbo, REMOTE_CHECK_RESULT_KEY),
                TranslatorUtils.toString(dbo, REMOTE_CHECK_MESSAGE_KEY));
    }

    private FileUploadResultType translateUploadResult(DBObject dbo, String name) {
        String result = TranslatorUtils.toString(dbo, name);
        return result == null ? null : FileUploadResultType.valueOf(result);
    }
}
