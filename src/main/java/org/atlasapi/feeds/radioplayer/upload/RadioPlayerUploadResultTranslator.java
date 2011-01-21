package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.persistence.mongo.MongoConstants.ID;

import org.joda.time.DateTime;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class RadioPlayerUploadResultTranslator {

    private ExceptionSummaryTranslator exceptionTranslator = new ExceptionSummaryTranslator();

    public DBObject toDBObject(RadioPlayerUploadResult result) {

        DBObject dbo = new BasicDBObject();

        TranslatorUtils.from(dbo, ID, id(result));
        TranslatorUtils.from(dbo, "filename", result.filename());
        TranslatorUtils.from(dbo, "success", result.wasSuccessful());
        TranslatorUtils.fromDateTime(dbo, "time", result.uploadTime());

        TranslatorUtils.from(dbo, "message", result.message());

        if (result.exception() != null) {
            TranslatorUtils.from(dbo, "exception", exceptionTranslator.toDBObject(result.exception()));
        }

        return dbo;
    }

    private String id(RadioPlayerUploadResult result) {
        return result.uploadTime().getMillis() + ":" + result.filename();
    }

    public RadioPlayerUploadResult fromDBObject(DBObject dbo) {

        Boolean success = TranslatorUtils.toBoolean(dbo, "success");
        String filename = TranslatorUtils.toString(dbo, "filename");
        DateTime time = TranslatorUtils.toDateTime(dbo, "time");

        DefaultRadioPlayerUploadResult result = success ? DefaultRadioPlayerUploadResult.successfulUpload(filename, time) : DefaultRadioPlayerUploadResult.failedUpload(filename, time);

        result.withMessage(TranslatorUtils.toString(dbo, "message"));

        if (dbo.containsField("exception")) {
            result.withCause(exceptionTranslator.fromDBObject((DBObject) dbo.get("exception")));
        }

        return result;
    }

}
