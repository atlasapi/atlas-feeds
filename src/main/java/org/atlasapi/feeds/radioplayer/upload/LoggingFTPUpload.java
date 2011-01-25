package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;

public class LoggingFTPUpload implements FTPUpload {

    private final AdapterLog log;
    private final FTPUpload delegate;

    public LoggingFTPUpload(AdapterLog log, FTPUpload delegate) {
        this.log = log;
        this.delegate = delegate;
    }
    
    @Override
    public FTPUploadResult call() throws Exception {
        FTPUploadResult result = delegate.call();
        if(FTPUploadResultType.FAILURE.equals(result.type())) {
            log(result);
        }
        return result;
    }

    private void log(FTPUploadResult result) {
        if(log != null) {
            log.record(new AdapterLogEntry(Severity.ERROR).withCause(result.exception()).withDescription(result.filename() + ":" + result.message()).withSource(getClass()));
        }
    }

}
