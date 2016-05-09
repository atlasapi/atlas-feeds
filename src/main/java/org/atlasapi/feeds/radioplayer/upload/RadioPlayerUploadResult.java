package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadResult;

import org.joda.time.LocalDate;

import static com.google.common.base.Preconditions.checkNotNull;

public class RadioPlayerUploadResult {

    private final RadioPlayerService service;
    private final FileUploadResult upload;
    private final LocalDate day;
    private final FileType type;

    public RadioPlayerUploadResult(
            FileType type,
            RadioPlayerService service,
            LocalDate day,
            FileUploadResult upload
    ) {
        this.type = type;
        this.day = checkNotNull(day);
        this.service = checkNotNull(service);
        this.upload = checkNotNull(upload);
    }

    public RadioPlayerService getService() {
        return service;
    }

    public FileUploadResult getUpload() {
        return upload;
    }

    public FileType getType() {
        return type;
    }

    public LocalDate getDay() {
        return day;
    }
}
