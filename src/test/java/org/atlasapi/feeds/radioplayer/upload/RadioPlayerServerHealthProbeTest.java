package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.health.RadioPlayerUploadServiceHealthProbe;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.persistence.MongoFileUploadResultStore;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DateTimeZones;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;

public class RadioPlayerServerHealthProbeTest {

    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final RadioPlayerService SERVICE = RadioPlayerServices.all.get("340");
    
    //TODO: remove mongo and mock RadioPlayerUploadResultStore
    public final static DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    public final RadioPlayerUploadServiceHealthProbe probe = new RadioPlayerUploadServiceHealthProbe("remote", new MongoFileUploadResultStore(mongo));
    private RadioPlayerUploadResultStore recorder = new UploadResultStoreBackedRadioPlayerResultStore(new MongoFileUploadResultStore(mongo));
    
    @BeforeClass
    public static void setup() {
        mongo.collection("uploads").ensureIndex(new BasicDBObjectBuilder().add("service",1).add("id", 1).add("time", -1).get());
    }
    
    @After
    public void tearDown() {
        mongo.collection("uploads").remove(new BasicDBObject());
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
        return new RadioPlayerUploadResult(PI, SERVICE, successDate.toLocalDate(), new FileUploadResult("remote", String.format("%s_%s_PI.xml", successDate.toString(DATE_FORMAT), SERVICE.getRadioplayerId()), successDate, type).withConnectionSuccess(connectionSuccess));
    }
    
}
