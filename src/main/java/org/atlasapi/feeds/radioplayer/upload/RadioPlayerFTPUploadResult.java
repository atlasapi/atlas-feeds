package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class RadioPlayerFTPUploadResult extends FTPUploadResult {

    private RadioPlayerService service;
    private LocalDate day;

    public RadioPlayerFTPUploadResult(String filename, DateTime dateTime, FTPUploadResultType success, RadioPlayerService service, LocalDate day) {
        super(filename, dateTime, success);
        this.service = service;
        this.day = day;
    }

    public RadioPlayerFTPUploadResult(FTPUploadResult upload, RadioPlayerService service, LocalDate localDate) {
        super(upload.filename(), upload.uploadTime(), upload.type());
        this.service = service;
        this.day = localDate;
        withCause(upload.exception());
        withConnectionSuccess(upload.successfulConnection());
        withMessage(upload.message());
    }

    public RadioPlayerService service() {
        return service;
    }

    public LocalDate day() {
        return day;
    }
    
}
