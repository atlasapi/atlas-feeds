package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.FAILURE;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.SUCCESS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;

import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DateTimeZones;
import com.mongodb.BasicDBObject;

public class RadioPlayerServerHealthProbeTest {


    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final RadioPlayerService SERVICE = RadioPlayerServices.all.get("340");
    
    public final DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    public final RadioPlayerServerHealthProbe probe = new RadioPlayerServerHealthProbe(mongo, FTPCredentials.forServer("hello").build());
    private RadioPlayerFTPUploadResultRecorder recorder = new MongoFTPUploadResultRecorder(mongo);
    
    @After
    public void tearDown() {
        mongo.collection("radioplayer").remove(new BasicDBObject());
    }
    
    @Test
    public void testProbe() {
        
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC)).withConnectionSuccess(true));
        
        ProbeResult result = probe.probe();
        
        assertThat(result.isFailure(), is(false));
        
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC)).withConnectionSuccess(false));
        
        result = probe.probe();
        
        assertThat(result.isFailure(), is(true));
        
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC)).withConnectionSuccess(true));
        
        result = probe.probe();
        
        assertThat(result.isFailure(), is(false));
        
    }

    
    public RadioPlayerFTPUploadResult successfulResult(DateTime successDate) {
        return result(successDate, SUCCESS);
    }
    
    public RadioPlayerFTPUploadResult failedResult(DateTime successDate) {
        return result(successDate, FAILURE);
    }

    public RadioPlayerFTPUploadResult result(DateTime successDate, FTPUploadResultType type) {
        return new RadioPlayerFTPUploadResult(String.format("%s_%s_PI.xml", successDate.toString(DATE_FORMAT), SERVICE.getRadioplayerId()), successDate, type, SERVICE, successDate.toLocalDate());
    }
    
}
