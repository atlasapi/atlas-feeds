package org.atlasapi.feeds.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.FAILURE;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;

import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;

public class LoggingFileUploader implements FileUploader {

    private final AdapterLog log;
    private final FileUploader delegate;

    public LoggingFileUploader(AdapterLog log, FileUploader delegate) {
        this.log = log;
        this.delegate = delegate;
    }

    @Override
    public FileUploadResult upload(FileUpload upload) throws Exception {
        FileUploadResult result = delegate.upload(upload);
        if (FAILURE.equals(result.type())) {
            log(result);
        }
        return result;
    }

    private void log(FileUploadResult result) {
        if (log != null) {
            AdapterLogEntry entry = new AdapterLogEntry(ERROR).withDescription(result.filename() + ":" + result.message()).withSource(getClass());
            log.record(result.exception() == null ? entry : entry.withCause(result.exception()));
        }
    }

}
