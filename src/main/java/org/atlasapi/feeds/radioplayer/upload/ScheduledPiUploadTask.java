package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadManager;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;

import com.metabroadcast.common.time.DayRangeGenerator;


public class ScheduledPiUploadTask extends ScheduledUploadTask {

    public ScheduledPiUploadTask(Iterable<UploadService> uploadServices,
            DayRangeGenerator dayRangeGenerator, Iterable<RadioPlayerService> services,
            UploadManager stateUpdater) {
        super(uploadServices, dayRangeGenerator, services, stateUpdater);
    }

    @Override
    public FileType fileType() {
        return FileType.PI;
    }

}
