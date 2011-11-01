package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class RadioPlayerFTPUploadResult extends FileUploadResult {

    private final RadioPlayerService service;
    private final LocalDate day;
    private FileUploadResultType processSuccess;

    public RadioPlayerFTPUploadResult(String filename, DateTime dateTime, FileUploadResultType success, RadioPlayerService service, LocalDate day) {
        super(filename, dateTime, success);
        this.service = service;
        this.day = day;
    }

    public RadioPlayerFTPUploadResult(FileUploadResult upload, RadioPlayerService service, LocalDate localDate) {
        super(upload.filename(), upload.uploadTime(), upload.type());
        this.service = service;
        this.day = localDate;
        withCause(upload.exception());
        withConnectionSuccess(upload.successfulConnection());
        withMessage(upload.message());
    }
    
    public RadioPlayerFTPUploadResult withProcessSuccess(FileUploadResultType processSuccess) {
        this.processSuccess = processSuccess;
        return this;
    }

    public RadioPlayerService service() {
        return service;
    }

    public LocalDate day() {
        return day;
    }
    
    public FileUploadResultType processSuccess() {
        return processSuccess;
    }
}
