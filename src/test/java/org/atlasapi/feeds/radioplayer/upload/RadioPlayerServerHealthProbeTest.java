package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;

import com.google.common.net.HostSpecifier;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DateTimeZones;
import com.mongodb.BasicDBObject;

public class RadioPlayerServerHealthProbeTest {


    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final RadioPlayerService SERVICE = RadioPlayerServices.all.get("340");
    
    public final DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    public final RadioPlayerServerHealthProbe probe = new RadioPlayerServerHealthProbe(mongo, RemoteServiceDetails.forServer(HostSpecifier.fromValid("127.0.0.1")).build());
    private RadioPlayerUploadResultStore recorder = new MongoFTPUploadResultStore(mongo);
    
    @After
    public void tearDown() {
        mongo.collection("radioplayer").remove(new BasicDBObject());
    }
    
    @Test
    public void testProbe() {
        
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC), true));
        
        ProbeResult result = probe.probe();
        
        assertThat(result.isFailure(), is(false));
        
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC), false));
        
        result = probe.probe();
        
        assertThat(result.isFailure(), is(true));
        
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC), true));
        
        result = probe.probe();
        
        assertThat(result.isFailure(), is(false));
        
    }

    
    private RadioPlayerUploadResult successfulResult(DateTime successDate, boolean connectionSuccess) {
        return result(successDate, SUCCESS, connectionSuccess);
    }
    
    private RadioPlayerUploadResult result(DateTime successDate, FileUploadResultType type, boolean connectionSuccess) {
        return new RadioPlayerUploadResult("aservice", SERVICE, successDate.toLocalDate(), new FileUploadResult(String.format("%s_%s_PI.xml", successDate.toString(DATE_FORMAT), SERVICE.getRadioplayerId()), successDate, type).withConnectionSuccess(connectionSuccess));
    }
    
}
