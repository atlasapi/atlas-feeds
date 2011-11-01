package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;

import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.ftp.FTPUploadResultTranslator;
import org.joda.time.DateTime;
import org.junit.Test;

import com.metabroadcast.common.time.DateTimeZones;
import com.mongodb.DBObject;

public class RadioPlayerUploadResultTranslatorTest {

    private final FTPUploadResultTranslator translator = new FTPUploadResultTranslator();
    
    @Test
    public void testCodingOfSuccessResult() {
        
        DateTime time = new DateTime(DateTimeZones.UTC);
        
        FileUploadResult result = new FileUploadResult("success", time, FileUploadResultType.SUCCESS).withMessage("SUCCESS");
        
        DBObject encoded = translator.toDBObject(result);
        
        FileUploadResult decoded = translator.fromDBObject(encoded);
        
        assertThat(decoded.type(), is(equalTo(result.type())));
        assertThat(decoded.filename(), is(equalTo(result.filename())));
        assertThat(decoded.uploadTime(), is(equalTo(result.uploadTime())));
        assertThat(decoded.message(), is(equalTo(result.message())));
        
    }

    @Test
    public void testCodingOfFailureResult() {
        
        DateTime time = new DateTime(DateTimeZones.UTC);
        
        Exception exception = null;
        try {
            new ArrayList<Object>().get(3);
        } catch(Exception e) {
            exception = e;
        }
        
        assertThat(exception, is(notNullValue()));
        
        FileUploadResult result = new FileUploadResult("failed", time, FileUploadResultType.FAILURE).withMessage("FAILURE").withCause(exception);
        
        DBObject encoded = translator.toDBObject(result);
        
        FileUploadResult decoded = translator.fromDBObject(encoded);
        
        assertThat(decoded.type(), is(equalTo(result.type())));
        assertThat(decoded.filename(), is(equalTo(result.filename())));
        assertThat(decoded.uploadTime(), is(equalTo(result.uploadTime())));
        assertThat(decoded.message(), is(equalTo(result.message())));
        
        assertThat(decoded.exceptionSummary().className(), is(equalTo(result.exceptionSummary().className())));
        assertThat(decoded.exceptionSummary().message(), is(equalTo(result.exceptionSummary().message())));
        assertThat(decoded.exceptionSummary().fullTrace(), is(equalTo(result.exceptionSummary().fullTrace())));
    }
}
