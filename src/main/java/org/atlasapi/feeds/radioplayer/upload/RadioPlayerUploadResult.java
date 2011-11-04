package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.joda.time.LocalDate;

public class RadioPlayerUploadResult {

    private final RadioPlayerService service;
    private final LocalDate day;
    private final String serviceId;
    private final FileUploadResult upload;

    public RadioPlayerUploadResult(String serviceId, RadioPlayerService service, LocalDate localDate, FileUploadResult upload) {
        this.serviceId = checkNotNull(serviceId);
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

    public String remoteService() {
        return serviceId;
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
            return service.equals(other.service) && serviceId.equals(other.serviceId) && day.equals(other.day) && upload.equals(other.upload);
        }
        return false;
    }
}
