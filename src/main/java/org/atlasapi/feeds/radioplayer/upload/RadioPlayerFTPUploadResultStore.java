package org.atlasapi.feeds.radioplayer.upload;

import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.joda.time.LocalDate;

public interface RadioPlayerFTPUploadResultStore {

    void record(RadioPlayerFTPUploadResult result);
    
    Set<RadioPlayerFTPUploadResult> resultsFor(RadioPlayerService service, LocalDate day);

}
