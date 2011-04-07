package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.failedUpload;

import java.util.List;
import java.util.Map;

import org.atlasapi.feeds.radioplayer.RadioPlayerFilenameMatcher;
import org.atlasapi.feeds.radioplayer.upload.CommonsFTPFileUploader.FileLastModified;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableMap;

public class RadioPlayerSuccessChecker implements Runnable {
    
    private final static String SUCCESS_DIR = "Processed";
    private final static String FAILURE_DIR = "Failed";
    
    private final CommonsFTPFileUploader ftp;
    private final RadioPlayerFTPUploadResultStore resultStore;
    private final AdapterLog log;

    public RadioPlayerSuccessChecker(CommonsFTPFileUploader ftp, RadioPlayerFTPUploadResultStore resultStore, AdapterLog log) {
        this.ftp = ftp;
        this.resultStore = resultStore;
        this.log = log;
    }
    
    @Override
    public void run() {
        try {
            checkSuccesses();
        } catch (Exception e) {
            log.record(new AdapterLogEntry(Severity.ERROR).withDescription("Problem checking RadioPlayer successes").withCause(e));
        }
    }
    
    public void checkSuccesses() {
        List<FileLastModified> failures = ftp.listDir(FAILURE_DIR);
        List<FileLastModified> successes = ftp.listDir(SUCCESS_DIR);
        
        Map<String, DateTime> successLookup = lookup(successes);
        
        for (FileLastModified file: failures) {
            if (! successLookup.containsKey(file.fileName())) {
                recordFailure(file.fileName(), "File has only ever failed");
            } else {
                DateTime lastSuccess = successLookup.get(file.fileName());
                if (file.lastModified().isAfter(lastSuccess)) {
                    recordFailure(file.fileName(), "File's most recent processing was a failure");
                }
            }
        }
    }
    
    private void recordFailure(String filename, String message) {
        filename = filename.replace(".xml", "");
        RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(filename);
        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {
            resultStore.record(new RadioPlayerFTPUploadResult(failedUpload(filename).withMessage(message), matcher.service().requireValue(), matcher.date().requireValue()));
        } else {
            log.record(new AdapterLogEntry(Severity.ERROR).withDescription("Unable to check the success of RadioPlayer upload for file "+filename+" as couldn't derive service and day").withSource(getClass()));
        }
    }
    
    private Map<String, DateTime> lookup(List<FileLastModified> files) {
        ImmutableMap.Builder<String, DateTime> map = ImmutableMap.builder();
        
        for (FileLastModified file: files) {
            map.put(file.fileName(), file.lastModified());
        }
        
        return map.build();
    }
}
