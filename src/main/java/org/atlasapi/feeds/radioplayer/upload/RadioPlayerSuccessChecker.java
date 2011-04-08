package org.atlasapi.feeds.radioplayer.upload;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerFilenameMatcher;
import org.atlasapi.feeds.radioplayer.upload.CommonsFTPFileUploader.FileLastModified;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

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

        Map<String, DateTime> failureLookup = lookup(failures);

        for (FileLastModified file : successes) {
            try {
                FTPUploadResultType processSuccess = FTPUploadResultType.SUCCESS;
                if (failureLookup.containsKey(file.fileName())) {
                    DateTime lastFailure = failureLookup.get(file.fileName());
                    if (file.lastModified().isBefore(lastFailure)) {
                        processSuccess = FTPUploadResultType.FAILURE;
                    }
                }

                for (RadioPlayerFTPUploadResult result : getCurrentResults(file.fileName())) {
                    resultStore.record(result.withProcessSuccess(processSuccess));
                }
            } catch (Exception e) {
                log.record(new AdapterLogEntry(Severity.ERROR).withDescription("Problem processing file: " + file).withCause(e));
            }
        }
    }

    private Set<RadioPlayerFTPUploadResult> getCurrentResults(String filename) {
        filename = filename.replace(".xml", "");
        RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(filename);
        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {
            return resultStore.resultsFor(matcher.service().requireValue(), matcher.date().requireValue());
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
