package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;

public class LoggingRadioPlayerUploader implements RadioPlayerUploader {

    private final AdapterLog log;
    private final RadioPlayerUploader delegate;

    public LoggingRadioPlayerUploader(AdapterLog log, RadioPlayerUploader delegate) {
        this.log = log;
        this.delegate = delegate;
    }
    
    @Override
    public RadioPlayerUploadResult upload(String filename, byte[] fileData) {
        RadioPlayerUploadResult status = delegate.upload(filename, fileData);
        if(!status.wasSuccessful()) {
            log(status);
        }
        return status;
    }

    private void log(RadioPlayerUploadResult status) {
        log.record(new AdapterLogEntry(Severity.WARN).withSource(getClass()).withCause(new Exception(status.exception().message())).withDescription(status.filename() + ": " + status.message()));
    }

}
