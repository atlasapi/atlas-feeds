package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.persistence.mongo.MongoConstants.ID;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.DBObject;

public class RadioPlayerFTPUploadResultTranslator {

    FTPUploadResultTranslator basicTranslator = new FTPUploadResultTranslator();

    public DBObject toDBObject(RadioPlayerFTPUploadResult result) {

        DBObject dbo = basicTranslator.toDBObject(result);

        TranslatorUtils.from(dbo, "serviceId", result.service());
        TranslatorUtils.from(dbo, "day", result.day());
        
        dbo.put(ID, id(result));

        return dbo;

    }

    private String id(RadioPlayerFTPUploadResult result) {
        return String.format("%s:%s:%s", result.type(), result.service(), result.day());
    }

    public RadioPlayerFTPUploadResult fromDBObject(DBObject dbo) {

        FTPUploadResult base = basicTranslator.fromDBObject(dbo);

        String service = TranslatorUtils.toString(dbo, "serviceId");
        String day = TranslatorUtils.toString(dbo, "day");
        
        return new RadioPlayerFTPUploadResult(base, service, day);
    }

}
