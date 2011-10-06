package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.persistence.mongo.MongoConstants.ID;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.ftp.FTPUploadResultTranslator;
import org.joda.time.LocalDate;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.DBObject;

public class RadioPlayerFTPUploadResultTranslator {

    FTPUploadResultTranslator basicTranslator = new FTPUploadResultTranslator();

    public DBObject toDBObject(RadioPlayerFTPUploadResult result) {

        DBObject dbo = basicTranslator.toDBObject(result);

        RadioPlayerFTPUploadResult rpResult = (RadioPlayerFTPUploadResult) result;

        TranslatorUtils.from(dbo, "serviceId", rpResult.service().getRadioplayerId());
        TranslatorUtils.fromLocalDate(dbo, "day", rpResult.day());
        if (result.processSuccess() != null) {
            TranslatorUtils.from(dbo, "processSuccess", result.processSuccess().toString());
        }

        dbo.put(ID, id(rpResult));

        return dbo;
    }

    private String id(RadioPlayerFTPUploadResult result) {
        return String.format("%s:%s:%s", result.type(), result.service().getRadioplayerId(), result.day().toString("yyyyMMdd"));
    }

    public RadioPlayerFTPUploadResult fromDBObject(DBObject dbo) {

        FileUploadResult base = basicTranslator.fromDBObject(dbo);

        RadioPlayerService service = RadioPlayerServices.all.get(TranslatorUtils.toInteger(dbo, "serviceId").toString());
        LocalDate day = TranslatorUtils.toLocalDate(dbo, "day");
        
        RadioPlayerFTPUploadResult result = new RadioPlayerFTPUploadResult(base, service, day);
        if (dbo.containsField("processSuccess")) {
            FileUploadResultType processSuccess = FileUploadResultType.valueOf(TranslatorUtils.toString(dbo, "processSuccess"));
            result.withProcessSuccess(processSuccess);
        }

        return result;
    }

}
