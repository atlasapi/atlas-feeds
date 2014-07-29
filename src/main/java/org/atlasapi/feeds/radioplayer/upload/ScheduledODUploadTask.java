package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.queue.InteractionManager;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;

import com.metabroadcast.common.time.DayRangeGenerator;


public class ScheduledODUploadTask extends ScheduledUploadTask {

    public ScheduledODUploadTask(Iterable<UploadService> uploadServices,
            DayRangeGenerator dayRangeGenerator, Iterable<RadioPlayerService> services,
            InteractionManager stateUpdater) {
        super(uploadServices, dayRangeGenerator, services, stateUpdater);
    }

    @Override
    FileType fileType() {
        return FileType.OD;
    }

}
