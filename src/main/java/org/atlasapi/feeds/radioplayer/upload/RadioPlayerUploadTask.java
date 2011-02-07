package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.SUCCESS;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.INFO;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadTask implements Runnable {

    private final Iterable<RadioPlayerService> services;
    private final RadioPlayerRecordingExecutor executor;
    private final FTPFileUploader uploader;
    
    private DayRangeGenerator dayRangeGenerator;
    private Iterable<LocalDate> dayRange;

    private RadioPlayerXMLValidator validator;
    private AdapterLog log;
    
    public RadioPlayerUploadTask(FTPFileUploader uploader, RadioPlayerRecordingExecutor executor, Iterable<RadioPlayerService> services, DayRangeGenerator dayRangeGenerator) {
        this.uploader = uploader;
        this.executor = executor;
		this.services = services;
        this.dayRangeGenerator = dayRangeGenerator;
    }
    
    public RadioPlayerUploadTask(FTPFileUploader uploader, RadioPlayerRecordingExecutor executor, Iterable<RadioPlayerService> services, Iterable<LocalDate> dayRange) {
        this.uploader = uploader;
        this.executor = executor;
        this.services = services;
        this.dayRange = dayRange;
    }
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        
        int serviceCount = Iterables.size(services);
        Iterable<LocalDate> days = dayRange != null ? dayRange : dayRangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
        log(String.format("Radioplayer Uploader starting for %s services for %s days", serviceCount, Iterables.size(days)), INFO);
        
        List<Callable<RadioPlayerFTPUploadResult>> uploadTasks = Lists.newArrayListWithCapacity(Iterables.size(days) * serviceCount);
        for (RadioPlayerService service : services) {
            for(LocalDate day : days) {
            	uploadTasks.add(new RadioPlayerFTPUploadTask(uploader, day.toDateTimeAtCurrentTime(DateTimeZones.UTC), service).withValidator(validator).withLog(log));
            }
        }
        
        ExecutorCompletionService<RadioPlayerFTPUploadResult> results = executor.submit(uploadTasks);

        int successes = 0;
        for (int i = 0; i < uploadTasks.size(); i++) {
            try {
				RadioPlayerFTPUploadResult result = results.take().get();
                if(SUCCESS.equals(result.type())) {
                    successes++;
                }
            } catch (InterruptedException e) {
                log("Radioplayer Uploader interrupted waiting for result.", WARN, e);
            } catch (ExecutionException e) {
                log("Radioplayer Uploader exception retrieving result", WARN, e);
            }
        }

        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        log(String.format("Radioplayer Uploader finished in %s, %s/%s successful.", runTime, successes, uploadTasks.size()), INFO);
    }
    
    private void log(String desc, Severity s) {
        log(desc, s, null);
    }
    
    private void log(String desc, Severity s, Exception e) {
        if(log != null) {
            AdapterLogEntry entry = new AdapterLogEntry(s).withDescription(desc).withSource(getClass());
            log.record(e != null ? entry.withCause(e) : entry);
        }
    }
    
    public RadioPlayerUploadTask withValidator(RadioPlayerXMLValidator validator) {
        this.validator = validator;
        return this;
    }
    
    public RadioPlayerUploadTask withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
}
