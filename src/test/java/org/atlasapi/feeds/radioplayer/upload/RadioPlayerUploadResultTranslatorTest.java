package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.junit.Test;

import com.metabroadcast.common.time.DateTimeZones;
import com.mongodb.DBObject;

public class RadioPlayerUploadResultTranslatorTest {

    private final RadioPlayerUploadResultTranslator translator = new RadioPlayerUploadResultTranslator();
    
    @Test
    public void testCodingOfSuccessResult() {
        
        DateTime time = new DateTime(DateTimeZones.UTC);
        
        RadioPlayerUploadResult result = DefaultRadioPlayerUploadResult.successfulUpload("success").withUploadTime(time).withMessage("SUCCESS");
        
        DBObject encoded = translator.toDBObject(result);
        
        RadioPlayerUploadResult decoded = translator.fromDBObject(encoded);
        
        assertThat(decoded.wasSuccessful(), is(equalTo(result.wasSuccessful())));
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
        
        RadioPlayerUploadResult result = DefaultRadioPlayerUploadResult.failedUpload("failed").withUploadTime(time).withMessage("FAILURE").withCause(exception);
        
        DBObject encoded = translator.toDBObject(result);
        
        RadioPlayerUploadResult decoded = translator.fromDBObject(encoded);
        
        assertThat(decoded.wasSuccessful(), is(equalTo(result.wasSuccessful())));
        assertThat(decoded.filename(), is(equalTo(result.filename())));
        assertThat(decoded.uploadTime(), is(equalTo(result.uploadTime())));
        assertThat(decoded.message(), is(equalTo(result.message())));
        
        assertThat(decoded.exception().className(), is(equalTo(result.exception().className())));
        assertThat(decoded.exception().message(), is(equalTo(result.exception().message())));
        assertThat(decoded.exception().fullTrace(), is(equalTo(result.exception().fullTrace())));
    }
}
