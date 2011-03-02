package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.FAILURE;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;

import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;

public class LoggingFTPUploader implements FTPFileUploader {

    private final AdapterLog log;
    private final FTPFileUploader delegate;

    public LoggingFTPUploader(AdapterLog log, FTPFileUploader delegate) {
        this.log = log;
        this.delegate = delegate;
    }

    @Override
    public FTPUploadResult upload(FTPUpload upload) throws Exception {
        FTPUploadResult result = delegate.upload(upload);
        if (FAILURE.equals(result.type())) {
            log(result);
        }
        return result;
    }

    private void log(FTPUploadResult result) {
        if (log != null) {
            AdapterLogEntry entry = new AdapterLogEntry(ERROR).withDescription(result.filename() + ":" + result.message()).withSource(getClass());
            log.record(result.exception() == null ? entry : entry.withCause(result.exception()));
        }
    }

}
