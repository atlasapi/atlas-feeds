package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;
import static org.atlasapi.persistence.logging.AdapterLogEntry.debugEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;

import org.atlasapi.feeds.radioplayer.RadioPlayerPiFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.LocalDate;

import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerPiUploadTask extends RadioPlayerUploadTask {

    public RadioPlayerPiUploadTask(Iterable<FileUploadService> remoteTargets, LocalDate day, RadioPlayerService service, AdapterLog log, Publisher publisher) {
        super(PI, remoteTargets, new RadioPlayerPiFeedSpec(service, day), log, publisher);
    }

    @Override
    protected void logNotItemsException(NoItemsException e) {
        if( log != null) {
            if (!spec.getDay().isAfter(new LocalDate(DateTimeZones.UTC).plusDays(1)) && !RadioPlayerServices.untracked.contains(spec.getService())) {
                log.record(errorEntry().withDescription("No items for " + spec).withSource(getClass()).withCause(e));
            } else {
                log.record(debugEntry().withDescription("No items for " + spec).withSource(getClass()).withCause(e));
            }
        }
    }
}
