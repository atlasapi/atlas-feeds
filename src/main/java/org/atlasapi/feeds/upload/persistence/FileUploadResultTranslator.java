package org.atlasapi.feeds.upload.persistence;

import org.atlasapi.feeds.radioplayer.upload.ExceptionSummaryTranslator;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class FileUploadResultTranslator {
    
    private static final String TRANSACTION_ID_KEY = "transactionId";
    public static final String FILENAME = "filename";
    public static final String SERVICE_KEY = "service";
    public static final String TIME_KEY = "time";
    
    private ExceptionSummaryTranslator exceptionTranslator = new ExceptionSummaryTranslator();

    public DBObject toDBObject(FileUploadResult result) {

        DBObject dbo = new BasicDBObject();

        TranslatorUtils.from(dbo, SERVICE_KEY, result.remote());
        TranslatorUtils.from(dbo, FILENAME, result.filename());
        TranslatorUtils.from(dbo, "type", result.type().toString());
        TranslatorUtils.fromDateTime(dbo, TIME_KEY, result.uploadTime());
        TranslatorUtils.from(dbo, "message", result.message());
        TranslatorUtils.from(dbo, "connected", result.successfulConnection());
        TranslatorUtils.from(dbo, TRANSACTION_ID_KEY, result.transactionId());

        if (result.exception() != null) {
            TranslatorUtils.from(dbo, "exception", exceptionTranslator.toDBObject(result.exceptionSummary()));
        }
        
        if (result.remoteProcessingResult() != null) {
            TranslatorUtils.from(dbo, "remoteCheck", result.remoteProcessingResult().toString());
        }
        
 
        return dbo;
    }

    public FileUploadResult fromDBObject(DBObject dbo) {

        FileUploadResult result = new FileUploadResult(
                TranslatorUtils.toString(dbo, SERVICE_KEY), 
                TranslatorUtils.toString(dbo, FILENAME), 
                TranslatorUtils.toDateTime(dbo, TIME_KEY), FileUploadResultType.valueOf(TranslatorUtils.toString(dbo,"type"))
        ).copyWithMessage(TranslatorUtils.toString(dbo, "message")).withConnectionSuccess(TranslatorUtils.toBoolean(dbo, "connected"));

        if (dbo.containsField("exception")) {
            result = result.withExceptionSummary(exceptionTranslator.fromDBObject((DBObject) dbo.get("exception")));
        }
        
        if (dbo.containsField("remoteCheck")) {
            result = result.withRemoteProcessingResult(FileUploadResultType.valueOf(TranslatorUtils.toString(dbo,"remoteCheck")));
        }
        
        if (dbo.containsField(TRANSACTION_ID_KEY)) {
            result.withTransactionId(TranslatorUtils.toString(dbo, TRANSACTION_ID_KEY));
        }

        return result;
    }

}
