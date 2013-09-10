package org.atlasapi.feeds.upload;

import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;

import org.atlasapi.persistence.logging.AdapterLog;

public class LoggingFileUploader implements FileUploader {

    public static LoggingFileUploader loggingUploader(AdapterLog log, FileUploader delegate) {
        return new LoggingFileUploader(log, delegate);
    }
    
    private final AdapterLog log;
    private final FileUploader delegate;

    public LoggingFileUploader(AdapterLog log, FileUploader delegate) {
        this.log = log;
        this.delegate = delegate;
    }

    @Override
    public FileUploaderResult upload(FileUpload upload) throws Exception {
        try {
           return delegate.upload(upload);
        } catch (Exception e) {
            log(upload, e);
            throw e;
        }
    }

    private void log(FileUpload upload, Exception e) {
        if (log != null) {
            log.record(errorEntry().withDescription(upload.getFilename() + ":" + e.getMessage()).withSource(getClass()));
        }
    }

}
