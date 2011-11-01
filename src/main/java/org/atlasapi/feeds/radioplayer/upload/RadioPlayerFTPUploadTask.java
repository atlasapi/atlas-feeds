package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.failedUpload;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.DEBUG;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedCompiler;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.LoggingFileUploader;
import org.atlasapi.feeds.upload.ValidatingFileUploader;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.joda.time.LocalDate;

import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerFTPUploadTask implements Callable<RadioPlayerFTPUploadResult> {

    private final FileUploader uploader;
    private final LocalDate day;
    private final RadioPlayerService service;

    private XMLValidator validator;
    private AdapterLog log;

    public RadioPlayerFTPUploadTask(FileUploader uploader, LocalDate day, RadioPlayerService service) {
        this.uploader = uploader;
        this.day = day;
        this.service = service;
    }

    @Override
    public RadioPlayerFTPUploadResult call() throws Exception {
        return new RadioPlayerFTPUploadResult(compileAndUpload(), service, day);
    }

    private FileUploadResult compileAndUpload() {
        String filename = String.format("%s_%s_PI.xml", day.toString("yyyyMMdd"), service.getRadioplayerId());
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RadioPlayerFeedCompiler.valueOf("PI").compileFeedFor(day, service, out);
            
            FileUploader delegate = new LoggingFileUploader(log, new ValidatingFileUploader(validator, uploader));
            return delegate.upload(new FileUpload(filename, out.toByteArray()));
            
        } catch (InterruptedException e) {
            log.record(new AdapterLogEntry(ERROR).withCause(e).withDescription("Upload of " + filename + " was interrupted").withSource(getClass()));
            return failedUpload(filename).withCause(e).withMessage("Task timed-out");
        }catch (NoItemsException e) {
            logNotItemsException(filename, e);
            return failedUpload(filename).withCause(e).withMessage(e.getMessage());
        } catch (Exception e) {
            if (log != null) {
                log.record(new AdapterLogEntry(ERROR).withDescription("Exception uploading file " + filename).withSource(getClass()).withCause(e));
            }
            return failedUpload(filename).withCause(e).withMessage(e.getMessage());
        }
    }

    private void logNotItemsException(String filename, NoItemsException e) {
        if( log != null) {
            if (!day.isAfter(new LocalDate(DateTimeZones.UTC).plusDays(1)) && !RadioPlayerServices.untracked.contains(service)) {
                log.record(new AdapterLogEntry(ERROR).withDescription("No items for " + filename).withSource(getClass()).withCause(e));
            } else {
                log.record(new AdapterLogEntry(DEBUG).withDescription("No items for " + filename).withSource(getClass()).withCause(e));
            }
        }
    }

    public RadioPlayerFTPUploadTask withValidator(XMLValidator validator) {
        this.validator = validator;
        return this;
    }

    public RadioPlayerFTPUploadTask withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
}
