package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedType;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.joda.time.DateTime;

public class RadioPlayerFTPUploadTask implements Callable<FTPUploadResult> {

    private final FTPClient client;
    private RadioPlayerXMLValidator validator;
    private AdapterLog log;
    private final DateTime day;
    private final RadioPlayerService service;
    private final KnownTypeQueryExecutor queryExecutor;

    public RadioPlayerFTPUploadTask(FTPClient client, DateTime day, RadioPlayerService service, KnownTypeQueryExecutor queryExecutor) {
        this.client = client;
        this.day = day;
        this.service = service;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public FTPUploadResult call() throws Exception {
        String filename = filename(service, day);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RadioPlayerFeedType.PI.compileFeedFor(day, service, queryExecutor, out);
            FTPUpload delegate = new LoggingFTPUpload(log, new ValidatingFTPFileUpload(validator, new FTPFileUpload()));
            // delegate = new RemoteCheckingFTPFileUpload(delegate);
            return delegate.upload(client, filename, out.toByteArray()); 
        } catch (Exception e) {
            if(log != null) {
                log.record(new AdapterLogEntry(ERROR).withDescription("Exception uploading file " + filename).withSource(getClass()).withCause(e));
            }
            return DefaultFTPUploadResult.failedUpload(filename).withCause(e).withMessage(e.getMessage());
        }

    }

    private String filename(RadioPlayerService service, DateTime day) {
        return String.format("%s_%s_PI.xml", day.toString("yyyyMMdd"), service.getRadioplayerId());
    }

    public RadioPlayerFTPUploadTask withValidator(RadioPlayerXMLValidator validator) {
        this.validator = validator;
        return this;
    }
    
    public RadioPlayerFTPUploadTask withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
}
