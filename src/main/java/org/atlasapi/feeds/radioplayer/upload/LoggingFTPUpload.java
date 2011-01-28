package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.FAILURE;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;

public class LoggingFTPUpload implements FTPUpload {

    private final AdapterLog log;
    private final FTPUpload delegate;

    public LoggingFTPUpload(AdapterLog log, FTPUpload delegate) {
        this.log = log;
        this.delegate = delegate;
    }
    
    @Override
    public FTPUploadResult upload(FTPClient client, String filename, byte[] fileData) {
        FTPUploadResult result = delegate.upload(client, filename, fileData);
        if(FAILURE.equals(result.type())) {
            log(result);
        }
        return result;
    }

    private void log(FTPUploadResult result) {
        if(log != null) {
            log.record(new AdapterLogEntry(ERROR).withCause(result.exception()).withDescription(result.filename() + ":" + result.message()).withSource(getClass()));
        }
    }

}
