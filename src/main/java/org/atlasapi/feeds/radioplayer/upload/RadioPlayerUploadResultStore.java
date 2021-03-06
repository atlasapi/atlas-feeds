package org.atlasapi.feeds.radioplayer.upload;


import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.joda.time.LocalDate;

public interface RadioPlayerUploadResultStore {

    void record(RadioPlayerUploadResult result);
    
    Iterable<FileUploadResult> resultsFor(FileType type, String remoteServiceId, RadioPlayerService service, LocalDate day);
    
    List<FileUploadResult> allSuccessfulResults(String remoteServiceId);

}
