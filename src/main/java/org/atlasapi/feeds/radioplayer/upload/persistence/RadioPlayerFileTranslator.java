package org.atlasapi.feeds.radioplayer.upload.persistence;

import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.MongoTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class RadioPlayerFileTranslator implements MongoTranslator<RadioPlayerFile> {

    private static final DateTimeFormatter dateFormat = ISODateTimeFormat.date();
    private static final String TYPE_KEY = "type";
    private static final String DATE_KEY = "date";
    private static final String SERVICE_KEY = "service";
    private static final String UPLOAD_SERVICE_KEY = "uploadService";

    @Override
    public DBObject toDBObject(RadioPlayerFile file) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, UPLOAD_SERVICE_KEY, file.uploadService().name());
        // Have to get string value of int so can pull service out of RadioPlayerService map on other side
        TranslatorUtils.from(dbo, SERVICE_KEY, String.valueOf(file.service().getRadioplayerId()));
        TranslatorUtils.from(dbo, DATE_KEY, file.date().toString(dateFormat));
        TranslatorUtils.from(dbo, TYPE_KEY, file.type().name());
        
        return dbo;
    }

    @Override
    public RadioPlayerFile fromDBObject(DBObject dbo) {
        return new RadioPlayerFile(
                UploadService.valueOf(TranslatorUtils.toString(dbo, UPLOAD_SERVICE_KEY)), 
                RadioPlayerServices.all.get(TranslatorUtils.toString(dbo, SERVICE_KEY)),
                FileType.valueOf(TranslatorUtils.toString(dbo, TYPE_KEY)), 
                LocalDate.parse(TranslatorUtils.toString(dbo, DATE_KEY), dateFormat)
        );
    }

}
