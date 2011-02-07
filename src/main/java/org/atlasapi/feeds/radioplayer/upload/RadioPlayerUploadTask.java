package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.SUCCESS;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.INFO;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFtpAwareExecutor.CallableWithClient;
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
    private final RadioPlayerFtpAwareExecutor executor;
    
    private DayRangeGenerator dayRangeGenerator;
    private Iterable<LocalDate> dayRange;

    private RadioPlayerXMLValidator validator;
    private AdapterLog log;
    
    public RadioPlayerUploadTask(RadioPlayerFtpAwareExecutor excutor, Iterable<RadioPlayerService> services, DayRangeGenerator dayRangeGenerator) {
        this.executor = excutor;
		this.services = services;
        this.dayRangeGenerator = dayRangeGenerator;
    }
    
    public RadioPlayerUploadTask(RadioPlayerFtpAwareExecutor excutor, Iterable<RadioPlayerService> services, Iterable<LocalDate> dayRange) {
        this.executor = excutor;
        this.services = services;
        this.dayRange = dayRange;
    }
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        
        int serviceCount = Iterables.size(services);
        Iterable<LocalDate> days = dayRange != null ? dayRange : dayRangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
        log(String.format("Radioplayer Uploader starting for %s services for %s days", serviceCount, Iterables.size(days)), INFO);
        
        List<CallableWithClient<RadioPlayerFTPUploadResult>> uploadTasks = Lists.newArrayList();
        for (RadioPlayerService service : services) {
            for(LocalDate day : days) {
            	uploadTasks.add(buildTask(service, day.toDateTimeAtCurrentTime(DateTimeZones.UTC)));
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

	private CallableWithClient<RadioPlayerFTPUploadResult> buildTask(final RadioPlayerService service, final DateTime day) {
		return new CallableWithClient<RadioPlayerFTPUploadResult>() {

			@Override
			public Callable<RadioPlayerFTPUploadResult> create(FTPClient client) {
				return new RadioPlayerFTPUploadTask(client, day, service).withValidator(validator).withLog(log);
			}
		};
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
