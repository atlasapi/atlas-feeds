package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.LocalDate;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;


public class RadioPlayerFile {

    private static final Joiner JOIN_ON_COLON = Joiner.on(':');
    
    private final UploadService uploadService;
    private final RadioPlayerService service;
    private final FileType type;
    private final LocalDate date;
    
    public RadioPlayerFile(UploadService uploadService, RadioPlayerService service, FileType type, LocalDate date) {
        this.uploadService = checkNotNull(uploadService);
        this.service = checkNotNull(service);
        this.type = checkNotNull(type);
        this.date = checkNotNull(date);
    }
    
    public UploadService uploadService() {
        return uploadService;
    }
    
    public RadioPlayerService service() {
        return service;
    }
    
    public FileType type() {
        return type;
    }
    
    public LocalDate date() {
        return date;
    }
    
    public String toKey() {
        return JOIN_ON_COLON.join(
                uploadService, 
                service.getRadioplayerId(), 
                date, 
                type
        );
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("uploadService", uploadService)
                .add("service", service)
                .add("type", type)
                .add("date", date)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(uploadService, service, type, date);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof RadioPlayerFile) {
            RadioPlayerFile other = (RadioPlayerFile) that;
            return (uploadService.equals(other.uploadService))
                    && service.equals(other.service)
                    && type.equals(other.type)
                    && date.equals(other.date);
        }
        
        return false;
    }
}
