package org.atlasapi.feeds.radioplayer.upload.queue;

import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.joda.time.DateTime;

import com.google.common.base.Objects;


public final class UploadTask extends QueueTask {

    public UploadTask(RadioPlayerFile file) {
        super(file);
    }
    
    public UploadTask(RadioPlayerFile file, DateTime timestamp) {
        super(file, timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(file());
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("file", file())
                .add("timestamp", timestamp())
                .toString();
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof UploadTask) {
            UploadTask other = (UploadTask) that;
            return file().equals(other.file())
                    && Objects.equal(timestamp(), other.timestamp());
        }
        
        return false;
    }
}
