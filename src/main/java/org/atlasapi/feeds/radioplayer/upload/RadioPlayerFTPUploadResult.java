package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class RadioPlayerFTPUploadResult implements FTPUploadResult {

    private final FTPUploadResult delegate;
    private RadioPlayerService service;
    private LocalDate day;

    public RadioPlayerFTPUploadResult(FTPUploadResult delegate, RadioPlayerService service, LocalDate day) {
        this.delegate = delegate;
        this.service = service;
        this.day = day;
    }
    
    @Override
    public String filename() {
        return delegate.filename();
    }

    @Override
    public FTPUploadResultType type() {
        return delegate.type();
    }

    @Override
    public DateTime uploadTime() {
        return delegate.uploadTime();
    }

    @Override
    public String message() {
        return delegate.message();
    }

    @Override
    public ExceptionSummary exceptionSummary() {
        return delegate.exceptionSummary();
    }

    @Override
    public Exception exception() {
        return delegate.exception();
    }

    public RadioPlayerService service() {
        return service;
    }

    public LocalDate day() {
        return day;
    }

}
