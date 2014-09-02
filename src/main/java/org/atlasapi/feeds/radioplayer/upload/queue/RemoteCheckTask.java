package org.atlasapi.feeds.radioplayer.upload.queue;

import java.util.Map;

import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;


public final class RemoteCheckTask extends QueueTask {

    private final Map<String, String> uploadDetails;
    
    public RemoteCheckTask(RadioPlayerFile file, Map<String, String> uploadDetails) {
        super(file);
        this.uploadDetails = ImmutableMap.copyOf(uploadDetails);
    }
    
    public RemoteCheckTask(RadioPlayerFile file, DateTime timestamp, Map<String, String> uploadDetails) {
        super(file, timestamp);
        this.uploadDetails = ImmutableMap.copyOf(uploadDetails);
    }
    
    public Map<String, String> uploadDetails() {
        return uploadDetails;
    }
    
    public Optional<String> getParameter(String key) {
        return Optional.fromNullable(uploadDetails.get(key));
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
                .add("uploadDetails", uploadDetails)
                .toString();
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof RemoteCheckTask) {
            RemoteCheckTask other = (RemoteCheckTask) that;
            return file().equals(other.file())
                    && uploadDetails.equals(other.uploadDetails)
                    && Objects.equal(timestamp(), other.timestamp());
        }
        
        return false;
    }
}
