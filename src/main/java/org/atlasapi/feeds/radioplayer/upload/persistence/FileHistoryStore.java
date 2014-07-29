package org.atlasapi.feeds.radioplayer.upload.persistence;

import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;

import com.google.common.base.Optional;


public interface FileHistoryStore {

    void store(FileHistory history);
    
    Optional<FileHistory> fetch(RadioPlayerFile file);
    
    UploadAttempt addUploadAttempt(RadioPlayerFile file, UploadAttempt attempt);
    
    /**
     * Finds a matching FileHistory record for the given file, and sets the enqueuedForUpload
     * flag to false, and the enqueuedForRemoteCheck flag to true, and writes the record back
     * @param file
     */
    void successfulUpload(RadioPlayerFile file);
}
