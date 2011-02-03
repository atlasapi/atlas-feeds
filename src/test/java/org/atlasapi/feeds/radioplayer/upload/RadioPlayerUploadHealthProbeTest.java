package org.atlasapi.feeds.radioplayer.upload;

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
        
        assertThat(Iterables.size(result.entries()), is(equalTo(1)));
        assertThat(Iterables.getOnlyElement(result.entries()).getType(), is(equalTo(ProbeResultType.INFO)));
        assertThat(Iterables.getOnlyElement(result.entries()).getValue(), is(equalTo("No Data")));
        
        DateTime successDate = new DateTime(DateTimeZones.UTC );
        DefaultFTPUploadResult uploadResult = new DefaultFTPUploadResult(String.format("%s_340_PI.xml", successDate.toString(DATE_FORMAT)), successDate, FTPUploadResultType.SUCCESS).withMessage("SUCCESS");
        recorder.record(new RadioPlayerFTPUploadResult(uploadResult, SERVICE, successDate.toLocalDate()));
        
        result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(1)));
        assertThat(Iterables.getOnlyElement(result.entries()).getType(), is(equalTo(ProbeResultType.SUCCESS)));
        
        DateTime failureDate = new DateTime(DateTimeZones.UTC);
        uploadResult = new DefaultFTPUploadResult(String.format("%s_340_PI.xml", failureDate.toString(DATE_FORMAT)), failureDate, FTPUploadResultType.FAILURE).withMessage("FAIL");
        recorder.record(new RadioPlayerFTPUploadResult(uploadResult, SERVICE, failureDate.toLocalDate()));
         
        result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(1)));
        assertThat(Iterables.getOnlyElement(result.entries()).getType(), is(equalTo(ProbeResultType.FAILURE)));
    }

    @Test
    public void testFailureFirst() {
        
        DateTime failureDate = new DateTime(DateTimeZones.UTC);
        DefaultFTPUploadResult uploadResult = new DefaultFTPUploadResult(String.format("%s_340_PI.xml", failureDate.toString(DATE_FORMAT)), failureDate, FTPUploadResultType.FAILURE).withMessage("FAIL");
        recorder.record(new RadioPlayerFTPUploadResult(uploadResult, SERVICE, failureDate.toLocalDate()));
        
        ProbeResult result = probe.probe();
        
        assertThat(Iterables.size(result.entries()), is(equalTo(1)));
        assertThat(Iterables.getOnlyElement(result.entries()).getType(), is(equalTo(ProbeResultType.FAILURE)));
//        assertThat(Iterables.getOnlyElement(result.entries()).getValue(), endsWith(String.format("No successes. Last failure %s. FAIL", failureDate.toString(DATE_TIME))));

    }
                                 
}
