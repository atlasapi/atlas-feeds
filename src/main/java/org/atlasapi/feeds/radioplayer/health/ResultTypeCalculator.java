package org.atlasapi.feeds.radioplayer.health;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;

import java.util.Set;

import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.time.Clock;


public class ResultTypeCalculator {
    
    private static final Predicate<UploadAttempt> IS_SUCCESS = new Predicate<UploadAttempt>() {
        @Override
        public boolean apply(UploadAttempt input) {
            return FileUploadResultType.SUCCESS.equals(input.remoteCheckResult());
        }
    };
    private static final Duration PI_NOT_TODAY_STALENESS = Duration.standardHours(4);
    private static final Duration PI_TODAY_STALENESS = Duration.standardMinutes(60);
    private static final Duration OD_TODAY_STALENESS = Duration.standardHours(4);
    
    private final Clock clock;
    
    public ResultTypeCalculator(Clock clock) {
        this.clock = checkNotNull(clock);
    }

    public ProbeResultType calculateResultType(FileHistory history) {
        UploadAttempt latestAttempt = history.getLatestUpload();
        UploadAttempt lastSuccess = getLastSuccess(history.uploadAttempts());
        if (isStale(history.file(), lastSuccess)) {
            return calculateResultType(latestAttempt);
        }
        return SUCCESS;
    }
    
    private UploadAttempt getLastSuccess(Set<UploadAttempt> uploadAttempts) {
        return Iterables.getOnlyElement(Ordering.natural().greatestOf(Iterables.filter(uploadAttempts, IS_SUCCESS), 1), null);
    }

    private boolean isStale(RadioPlayerFile file, UploadAttempt lastSuccess) {
        if (lastSuccess == null) {
            return true;
        }
        if (FileType.PI.equals(file.type())) {
            if (isToday(file.date())) {
                if (olderThan(lastSuccess.uploadTime(), PI_TODAY_STALENESS)) {
                    return true;
                } 
            } else {
                if (olderThan(lastSuccess.uploadTime(), PI_NOT_TODAY_STALENESS)) {
                    return true;
                }
            }
        } else if (FileType.OD.equals(file.type())) {
            if (isToday(file.date())) {
                if (olderThan(lastSuccess.uploadTime(), OD_TODAY_STALENESS)) {
                    return true;
                } 
            }
        }
        return false;
    }

    private boolean isToday(LocalDate day) {
        return day.isEqual(clock.now().toLocalDate());
    }
    
    private boolean olderThan(DateTime uploadTime, Duration stalenessWindow) {
        return uploadTime.plus(stalenessWindow).isBefore(clock.now());
    }

    private ProbeResultType calculateResultType(UploadAttempt latest) {
        if (FileUploadResultType.FAILURE.equals(latest.uploadResult())) {
            return ProbeResultType.FAILURE;
        } else if (FileUploadResultType.SUCCESS.equals(latest.uploadResult())) {
            return calculateFromRemoteResult(latest);
        } else {
            return ProbeResultType.INFO;
        }
    }

    private ProbeResultType calculateFromRemoteResult(UploadAttempt latest) {
        if (FileUploadResultType.FAILURE.equals(latest.remoteCheckResult())) {
            return ProbeResultType.FAILURE;
        } else if (FileUploadResultType.SUCCESS.equals(latest.remoteCheckResult())) {
            return calculateFromRemoteResult(latest);
        } else {
            return ProbeResultType.INFO;
        }
    }
}
