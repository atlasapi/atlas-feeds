package org.atlasapi.feeds.radioplayer.health;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;
import static org.atlasapi.feeds.upload.FileUploadResult.DATE_ORDERING;

import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.time.Clock;


public class StateChecker {
    private static final Predicate<FileUploadResult> IS_REMOTE_SUCCESS = new Predicate<FileUploadResult>() {
        @Override
        public boolean apply(FileUploadResult input) {
            return FileUploadResultType.SUCCESS.equals(input.type()) 
                    && FileUploadResultType.SUCCESS.equals(input.remoteProcessingResult());
        }
    };

    private static final Duration FAILURE_WINDOW = Duration.standardHours(4).plus(Duration.standardMinutes(25));
    private static final Duration PI_NOT_TODAY_STALENESS = Duration.standardHours(4);
    private static final Duration PI_TODAY_STALENESS = Duration.standardMinutes(60);
    
    protected static final String DATE_TIME = "dd/MM/yy HH:mm:ss";
    
    private final Clock clock;
    
    public StateChecker(Clock clock) {
        this.clock = checkNotNull(clock);
    }

    public FileUploadResult mostRecentSuccess(List<? extends FileUploadResult> results) {
        return Iterables.get(Iterables.filter(results, IS_REMOTE_SUCCESS), 0, null);
    }

    public List<? extends FileUploadResult> orderByDate(Iterable<? extends FileUploadResult> results) {
        return DATE_ORDERING.reverse().immutableSortedCopy(results);
    }

    public ProbeResultType entryResultType(FileUploadResult mostRecentSuccess, FileUploadResult mostRecent, LocalDate day, FileType type, RadioPlayerService service) {
        if (mostRecentSuccess != null) {
            if (FileUploadResultType.SUCCESS.equals(mostRecent.remoteProcessingResult()) && FileType.OD == type) {
                return SUCCESS;
            }
            if (!isStale(mostRecentSuccess)) {
                if (FileUploadResultType.SUCCESS.equals(mostRecent.remoteProcessingResult())) {
                    return probeResultTypeFrom(mostRecent, day, type, service);
                }
                return INFO;
            }
            return FAILURE;
        } 
        if (!isStale(mostRecent)) {
            return INFO;
        } 
        if (FileUploadResultType.FAILURE.equals(mostRecent.remoteProcessingResult())) {
            return FAILURE;
        }
        return probeResultTypeFrom(mostRecent, day, type, service);
    }

    private boolean isStale(FileUploadResult mostRecent) {
        return olderThan(mostRecent, FAILURE_WINDOW);
    }

    private ProbeResultType probeResultTypeFrom(FileUploadResult result, LocalDate day, FileType type, RadioPlayerService service) {
        switch (result.type()) {
        case SUCCESS:
            if (FileType.PI == type && (isToday(day) && olderThan(result, PI_TODAY_STALENESS) || olderThan(result, PI_NOT_TODAY_STALENESS))) {
                return FAILURE;
            }
            return SUCCESS;
        case FAILURE:
            if (day.isAfter(result.uploadTime().toLocalDate().plusDays(1)) || RadioPlayerServices.untracked.contains(service)) {
                return INFO;
            } else {
                return FAILURE;
            }
        default:
            return INFO;
        }
    }

    private boolean olderThan(FileUploadResult mostRecent, Duration todayStaleness) {
        return mostRecent.uploadTime().plus(todayStaleness).isBefore(clock.now());
    }

    private boolean isToday(LocalDate day) {
        return day.isEqual(clock.now().toLocalDate());
    }
}
