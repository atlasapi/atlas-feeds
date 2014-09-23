package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;


public abstract class QueueTask {

    private final RadioPlayerFile file;
    private boolean currentlyProcessing = false;
    
    private DateTime timestamp = null;
    
    public QueueTask(RadioPlayerFile file) {
        this(file, null);
    }
    
    public QueueTask(RadioPlayerFile file, DateTime timestamp) {
        this.file = checkNotNull(file);
        this.timestamp = timestamp;
    }
    
    public RadioPlayerFile file() {
        return file;
    }
    
    public UploadService uploadService() {
        return file.uploadService();
    }
    
    public RadioPlayerService service() {
        return file.service();
    }
    
    public FileType type() {
        return file.type();
    }
    
    public LocalDate date() {
        return file.date();
    }
    
    public void setTimestamp(DateTime timestamp) {
        this.timestamp = checkNotNull(timestamp);
    }
    
    public DateTime timestamp() {
        return timestamp;
    }
    
    public void setCurrentlyProcessing(boolean isProcessing) {
        this.currentlyProcessing = isProcessing;
    }
    
    public boolean isCurrentlyProcessing() {
        return currentlyProcessing;
    }    
}
