package org.atlasapi.feeds.radioplayer.upload.persistence;

import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;

import com.google.common.base.Optional;


public interface FileHistoryStore {

    void store(FileHistory file);
    
    Optional<FileHistory> fetch(RadioPlayerFile file);
    
    UploadAttempt addUploadAttempt(RadioPlayerFile file, UploadAttempt attempt);
}
