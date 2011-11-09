package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.joda.time.LocalDate;

public class RadioPlayerUploadResult {

    private final RadioPlayerService service;
    private final LocalDate day;
    private final FileUploadResult upload;

    public RadioPlayerUploadResult(RadioPlayerService service, LocalDate localDate, FileUploadResult upload) {
        this.service = checkNotNull(service);
        this.day = checkNotNull(localDate);
        this.upload = checkNotNull(upload);
    }

    public RadioPlayerService service() {
        return service;
    }

    public LocalDate day() {
        return day;
    }

    public FileUploadResult getUpload() {
        return upload;
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof RadioPlayerUploadResult) {
            RadioPlayerUploadResult other = (RadioPlayerUploadResult) that;
            return service.equals(other.service) && day.equals(other.day) && upload.equals(other.upload);
        }
        return false;
    }
}
