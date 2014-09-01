package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.ScheduledPiUploadTask;
import org.atlasapi.feeds.radioplayer.upload.ScheduledUploadTask;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadManager;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.DayRangeGenerator;


public class ScheduledUploadTaskTest {

    private UploadManager stateUpdater = Mockito.mock(UploadManager.class);
    private Iterable<UploadService> uploadServices = ImmutableSet.of(UploadService.HTTPS);
    private DayRangeGenerator dayRangeGenerator = new DayRangeGenerator(DateTimeZone.UTC).withLookAhead(1).withLookBack(1);
    private Iterable<RadioPlayerService> services = RadioPlayerServices.untracked;
    private final ScheduledUploadTask task = new ScheduledPiUploadTask(uploadServices, dayRangeGenerator, services, stateUpdater);
    
    @Test
    public void testGeneratesCorrectTaskSet() {
        task.run();
        
        for (UploadService uploadService : uploadServices) {
            for (RadioPlayerService service : services) {
                for (LocalDate day : dayRangeGenerator.generate(new LocalDate(DateTimeZone.UTC))) {
                    Mockito.verify(stateUpdater).enqueueUploadTask(new UploadTask(new RadioPlayerFile(uploadService, service, FileType.PI, day)));
                }
            }
        }
    }

}