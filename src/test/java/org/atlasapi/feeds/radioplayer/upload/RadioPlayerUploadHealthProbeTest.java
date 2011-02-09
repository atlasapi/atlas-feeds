package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.FAILURE;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.mongodb.BasicDBObject;

public class RadioPlayerUploadHealthProbeTest {

    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final RadioPlayerService SERVICE = RadioPlayerServices.all.get("340");
    
    //    private static final String DATE_TIME = "dd/MM/yy HH:mm:ss";
    public final DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    public final RadioPlayerUploadHealthProbe probe = new RadioPlayerUploadHealthProbe(mongo, SERVICE, new DayRangeGenerator().withLookAhead(0).withLookBack(0));
    private RadioPlayerFTPUploadResultRecorder recorder = new MongoFTPUploadResultRecorder(mongo);
    
    @After
    public void tearDown() {
        mongo.collection("radioplayer").remove(new BasicDBObject());
    }
    
    @Test
    public void testProbe() {

        ProbeResult result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(2)));
        assertThat(Iterables.getFirst(result.entries(), null).getType(), is(equalTo(ProbeResultType.INFO)));
        assertThat(Iterables.getFirst(result.entries(), null).getValue(), is(equalTo("No Data")));
        
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC)));
        
        result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(2)));
        assertThat(Iterables.getFirst(result.entries(), null).getType(), is(equalTo(ProbeResultType.SUCCESS)));
        
        recorder.record(failedResult(new DateTime(DateTimeZones.UTC)));
         
        result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(2)));
        assertThat(Iterables.getFirst(result.entries(), null).getType(), is(equalTo(ProbeResultType.FAILURE)));
        
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC)));
        
        result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(2)));
        assertThat(Iterables.getFirst(result.entries(), null).getType(), is(equalTo(ProbeResultType.SUCCESS)));    
    }

    @Test
    public void testSuccessTimesOut() {
     
        recorder.record(successfulResult(new DateTime(DateTimeZones.UTC).minusMinutes(25)));
        
        ProbeResult result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(2)));
        assertThat(Iterables.getFirst(result.entries(), null).getType(), is(equalTo(ProbeResultType.FAILURE)));
        
    }
    
    @Test
    public void testFutureFailureIsInfo() {
        
        DateTime futureDay = new DateTime().plusDays(4);
        FTPUploadResult upResult = new FTPUploadResult(String.format("%s_%s_PI.xml", futureDay.toString(DATE_FORMAT), SERVICE.getRadioplayerId()), new DateTime(DateTimeZones.UTC), FAILURE);
        RadioPlayerFTPUploadResult rpResult = new RadioPlayerFTPUploadResult(upResult.filename(), upResult.uploadTime(), upResult.type(), SERVICE, futureDay.toLocalDate());
        
        recorder.record(rpResult);
        
        ProbeResult result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(2)));
        assertThat(Iterables.getFirst(result.entries(), null).getType(), is(equalTo(ProbeResultType.INFO)));
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
