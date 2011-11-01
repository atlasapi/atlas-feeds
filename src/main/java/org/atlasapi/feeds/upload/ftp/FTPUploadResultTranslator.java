package org.atlasapi.feeds.upload.ftp;

import static com.metabroadcast.common.persistence.mongo.MongoConstants.ID;

import org.atlasapi.feeds.radioplayer.upload.ExceptionSummaryTranslator;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class FTPUploadResultTranslator {

    private ExceptionSummaryTranslator exceptionTranslator = new ExceptionSummaryTranslator();

    public DBObject toDBObject(FileUploadResult result) {

        DBObject dbo = new BasicDBObject();

        TranslatorUtils.from(dbo, ID, id(result));
        TranslatorUtils.from(dbo, "filename", result.filename());
        TranslatorUtils.from(dbo, "type", result.type().toString());
        TranslatorUtils.fromDateTime(dbo, "time", result.uploadTime());

        TranslatorUtils.from(dbo, "message", result.message());

        TranslatorUtils.from(dbo, "connected", result.successfulConnection());

        if (result.exception() != null) {
            TranslatorUtils.from(dbo, "exception", exceptionTranslator.toDBObject(result.exceptionSummary()));
        }

        return dbo;
    }

    private String id(FileUploadResult result) {
        return result.type() + ":" + result.filename();
    }

    public FileUploadResult fromDBObject(DBObject dbo) {

        FileUploadResult result = new FileUploadResult(TranslatorUtils.toString(dbo, "filename"), TranslatorUtils.toDateTime(dbo, "time"), FileUploadResultType.valueOf(TranslatorUtils.toString(dbo,
                "type")));

        result.withMessage(TranslatorUtils.toString(dbo, "message"));
        result.withConnectionSuccess(TranslatorUtils.toBoolean(dbo, "connected"));

        if (dbo.containsField("exception")) {
            result.withExceptionSummary(exceptionTranslator.fromDBObject((DBObject) dbo.get("exception")));
        }

        return result;
    }

}
