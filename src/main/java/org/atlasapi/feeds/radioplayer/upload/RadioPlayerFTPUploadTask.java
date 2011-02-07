package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.failedUpload;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedCompiler;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.joda.time.DateTime;

import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerFTPUploadTask implements Callable<RadioPlayerFTPUploadResult> {

    private final FTPClient client;
    private RadioPlayerXMLValidator validator;
    private AdapterLog log;
    private final DateTime day;
    private final RadioPlayerService service;

    public RadioPlayerFTPUploadTask(FTPClient client, DateTime day, RadioPlayerService service) {
        this.client = client;
        this.day = day;
        this.service = service;
    }

    @Override
    public RadioPlayerFTPUploadResult call() throws Exception {
        String filename = filename(service, day);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RadioPlayerFeedCompiler.valueOf("PI").compileFeedFor(day, service, out);
            FTPUpload delegate = new LoggingFTPUpload(log, new ValidatingFTPFileUpload(validator, new FTPFileUpload()));
            return wrap(delegate.upload(client, filename, out.toByteArray()));
        } catch (NoItemsException e) {
            if(log != null && !day.isAfter(new DateTime(DateTimeZones.UTC).plusDays(1))) {
                log.record(new AdapterLogEntry(ERROR).withDescription("Exception uploading file " + filename).withSource(getClass()).withCause(e));
            }
            return wrap(failedUpload(filename).withCause(e).withMessage(e.getMessage()));
        } catch (Exception e) {
            if (log != null) {
                log.record(new AdapterLogEntry(ERROR).withDescription("Exception uploading file " + filename).withSource(getClass()).withCause(e));
            }
            return wrap(failedUpload(filename).withCause(e).withMessage(e.getMessage()));
        }

    }

    public RadioPlayerFTPUploadResult wrap(FTPUploadResult upload) {
        return new RadioPlayerFTPUploadResult(upload, service, day.toLocalDate());
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
