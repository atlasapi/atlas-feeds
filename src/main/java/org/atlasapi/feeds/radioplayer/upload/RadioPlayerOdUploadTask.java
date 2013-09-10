package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.persistence.logging.AdapterLogEntry.debugEntry;

import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerOdFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;

public class RadioPlayerOdUploadTask extends RadioPlayerUploadTask {
    
    public RadioPlayerOdUploadTask(Iterable<FileUploadService> remoteTargets, Optional<DateTime> since, LocalDate day, RadioPlayerService service, Set<String> uris, AdapterLog log, Publisher publisher) {
        super(OD, remoteTargets, new RadioPlayerOdFeedSpec(service, day, since, uris), log, publisher);
    }

    @Override
    protected void logNotItemsException(NoItemsException e) {
        if( log != null) {
            log.record(debugEntry().withDescription("No items for " + spec).withSource(getClass()).withCause(e));
        }
    }
}
