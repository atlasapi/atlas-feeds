package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.LocalDate;

import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadTaskBuilder {

    private final FTPFileUploader uploader;
    private final RadioPlayerRecordingExecutor executor;
    private XMLValidator validator;
    private AdapterLog log;

    public RadioPlayerUploadTaskBuilder(FTPFileUploader uploader, RadioPlayerRecordingExecutor executor) {
        this.uploader = uploader;
        this.executor = executor;
    }
    
    public RadioPlayerUploadTaskBuilder withValidator(XMLValidator validator) {
        this.validator = validator;
        return this;
    }
    
    public RadioPlayerUploadTaskBuilder withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
    
    public RadioPlayerUploadTask newTask(Iterable<RadioPlayerService> services, DayRangeGenerator dayGenerator) {
        return new RadioPlayerUploadTask(uploader, executor, services, dayGenerator).withLog(log).withValidator(validator);
    }
    
    public RadioPlayerUploadTask newTask(Iterable<RadioPlayerService> services, Iterable<LocalDate> days) {
        return new RadioPlayerUploadTask(uploader, executor, services, days).withLog(log).withValidator(validator);
    }
    
}
