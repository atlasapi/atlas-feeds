package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerFilenameMatcher;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.ftp.CommonsDirectoryLister;
import org.atlasapi.feeds.upload.ftp.CommonsDirectoryLister.FileLastModified;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class RadioPlayerSuccessChecker implements Runnable {

    private final static String SUCCESS_DIR = "Processed";
    private final static String FAILURE_DIR = "Failed";

    private final CommonsDirectoryLister ftp;
    private final RadioPlayerUploadResultStore resultStore;
    private final AdapterLog log;
    private final String remoteService;

    public RadioPlayerSuccessChecker(String remoteService, CommonsDirectoryLister ftp, RadioPlayerUploadResultStore resultStore, AdapterLog log) {
        this.remoteService = remoteService;
        this.ftp = ftp;
        this.resultStore = resultStore;
        this.log = log;
    }

    @Override
    public void run() {
        try {
            checkSuccesses();
        } catch (Exception e) {
            log.record(errorEntry().withDescription("Exception checking remote processing for " + remoteService).withCause(e));
        }
    }

    public void checkSuccesses() {
        List<FileLastModified> failures = ftp.listDir(FAILURE_DIR);
        List<FileLastModified> successes = ftp.listDir(SUCCESS_DIR);

        Map<String, DateTime> failureLookup = lookup(failures);

        for (FileLastModified file : successes) {
            try {
                FileUploadResultType processSuccess = FileUploadResultType.SUCCESS;
                if (failureLookup.containsKey(file.fileName())) {
                    DateTime lastFailure = failureLookup.get(file.fileName());
                    if (file.lastModified().isBefore(lastFailure)) {
                        processSuccess = FileUploadResultType.FAILURE;
                    }
                }

                RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(file.fileName().replace(".xml", ""));
                if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {
                    for (FileUploadResult result : getCurrentResults(matcher)) {
                        resultStore.record(radioPlayerResult(matcher, result.withRemoteProcessingResult(processSuccess)));
                    }
                }
                
            } catch (Exception e) {
                log.record(errorEntry().withSource(getClass()).withDescription("Exception for service %s processing file %s", remoteService, file).withCause(e));
            }
        }
    }

    private RadioPlayerUploadResult radioPlayerResult(RadioPlayerFilenameMatcher matcher, FileUploadResult result) {
        return new RadioPlayerUploadResult(matcher.type().requireValue(), matcher.service().requireValue(), matcher.date().requireValue(), result);
    }

    private Set<FileUploadResult> getCurrentResults(RadioPlayerFilenameMatcher matcher) {
        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {
            return ImmutableSet.copyOf(resultStore.resultsFor(matcher.type().requireValue(), remoteService, matcher.service().requireValue(), matcher.date().requireValue()));
        }
        return ImmutableSet.of();
    }

    private Map<String, DateTime> lookup(List<FileLastModified> files) {
        ImmutableMap.Builder<String, DateTime> map = ImmutableMap.builder();

        for (FileLastModified file : files) {
            map.put(file.fileName(), file.lastModified());
        }

        return map.build();
    }
}
