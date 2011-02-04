package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.SUCCESSFUL;
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

public class RadioPlayerUploadTask implements Runnable {

    private final Iterable<RadioPlayerService> services;
    private RadioPlayerXMLValidator validator;
    private RadioPlayerFTPUploadResultRecorder recorder;
    private AdapterLog log;
    private int lookAhead = 0;
    private int lookBack = 0;
	private final RadioPlayerFtpAwareExecutor excutor;
    
    public RadioPlayerUploadTask(RadioPlayerFtpAwareExecutor excutor, Iterable<RadioPlayerService> services) {
        this.excutor = excutor;
		this.services = services;
    }
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        int serviceCount = Iterables.size(services);
        int days = lookBack + lookAhead + 1;

        log(String.format("Radioplayer Uploader starting for %s services for %s days", serviceCount, days), INFO);
        
        
        List<CallableWithClient<RadioPlayerFTPUploadResult>> uploadTasks = Lists.newArrayList();
        for (RadioPlayerService service : services) {
            DateTime day = new LocalDate().toInterval(DateTimeZones.UTC).getStart().minusDays(lookBack);
            for (int i = 0; i < days; i++, day = day.plusDays(1)) {
            	uploadTasks.add(buildTask(service, day));
            }
        }
        
        ExecutorCompletionService<RadioPlayerFTPUploadResult> results = excutor.submit(uploadTasks);

        int successes = 0;
        for (int i = 0; i < uploadTasks.size(); i++) {
            try {
				RadioPlayerFTPUploadResult result = results.take().get();
                recorder.record(result);
                if(SUCCESSFUL.apply(result)) {
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

    public RadioPlayerUploadTask withResultRecorder(RadioPlayerFTPUploadResultRecorder recorder) {
        this.recorder = recorder;
        return this;
    }

    public RadioPlayerUploadTask withValidator(RadioPlayerXMLValidator validator) {
        this.validator = validator;
        return this;
    }

    public RadioPlayerUploadTask withLookAhead(int lookAhead) {
        this.lookAhead = lookAhead;
        return this;
    }

    public RadioPlayerUploadTask withLookBack(int lookBack) {
        this.lookBack = lookBack;
        return this;
    }
    
    public RadioPlayerUploadTask withLog(AdapterLog log) {
        this.log = log;
        return this;
    }
}
