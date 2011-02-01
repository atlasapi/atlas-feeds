package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.persistence.mongo.MongoConstants.ID;

import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class FTPUploadResultTranslator {

    private ExceptionSummaryTranslator exceptionTranslator = new ExceptionSummaryTranslator();

    public DBObject toDBObject(FTPUploadResult result) {

        DBObject dbo = new BasicDBObject();

        TranslatorUtils.from(dbo, ID, id(result));
        TranslatorUtils.from(dbo, "filename", result.filename());
        TranslatorUtils.from(dbo, "type", result.type().toString());
        TranslatorUtils.fromDateTime(dbo, "time", result.uploadTime());

        TranslatorUtils.from(dbo, "message", result.message());

        if (result.exception() != null) {
            TranslatorUtils.from(dbo, "exception", exceptionTranslator.toDBObject(result.exceptionSummary()));
        }

        return dbo;
    }

    private String id(FTPUploadResult result) {
        return result.type()+":"+result.filename();
    }

    public FTPUploadResult fromDBObject(DBObject dbo) {

        DefaultFTPUploadResult result = new DefaultFTPUploadResult(TranslatorUtils.toString(dbo, "filename"), TranslatorUtils.toDateTime(dbo, "time"), FTPUploadResultType.valueOf(TranslatorUtils.toString(dbo, "type")));

        result.withMessage(TranslatorUtils.toString(dbo, "message"));

        if (dbo.containsField("exception")) {
            result.withExceptionSummary(exceptionTranslator.fromDBObject((DBObject) dbo.get("exception")));
        }

        return result;
    }

}
