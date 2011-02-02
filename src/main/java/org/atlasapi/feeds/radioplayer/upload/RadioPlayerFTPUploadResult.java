package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

public class RadioPlayerFTPUploadResult implements FTPUploadResult {

    private final FTPUploadResult delegate;
    private final String service;
    private final String day;

    public RadioPlayerFTPUploadResult(FTPUploadResult delegate, String serviceId, String day) {
        this.delegate = delegate;
        this.service = serviceId;
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

    public String service() {
        return service;
    }

    public String day() {
        return day;
    }

}
