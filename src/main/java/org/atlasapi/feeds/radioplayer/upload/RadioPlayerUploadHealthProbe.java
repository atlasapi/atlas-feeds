package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;
import static org.atlasapi.feeds.upload.FileUploadResult.DATE_ORDERING;

import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadHealthProbe implements HealthProbe {

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
    
    protected final RadioPlayerUploadResultStore store;
    protected final DayRangeGenerator rangeGenerator;
    
    private final String remoteServiceId;
    private final RadioPlayerService service;
    private final Clock clock;

    public RadioPlayerUploadHealthProbe(Clock clock, String remoteServiceId, RadioPlayerUploadResultStore store, RadioPlayerService service, DayRangeGenerator dayRangeGenerator) {
        this.clock = clock;
        this.remoteServiceId = remoteServiceId;
        this.store = store;
        this.service = service;
        this.rangeGenerator = dayRangeGenerator;
    }

    @Override
    public ProbeResult probe() {
        ProbeResult result = new ProbeResult(service.getName());
        
        DayRange dayRange = rangeGenerator.generate(clock.now().toLocalDate());
        
        for (LocalDate day : dayRange) {
            result.addEntry(entryFor(day, PI, store.resultsFor(PI, remoteServiceId, service, day)));
            result.addEntry(entryFor(day, OD, store.resultsFor(OD, remoteServiceId, service, day)));
        }

        result.addEntry(uploadAllPi());

        return result;
    }

    private ProbeResultEntry entryFor(LocalDate day, FileType type, Iterable<? extends FileUploadResult> results) {
        String filename = linkedFilename(type, day) + uploadButton(type, day);
        
        if (Iterables.isEmpty(results)) {
            return new ProbeResultEntry(INFO, filename, "No Data");
        }
        List<? extends FileUploadResult> dateOrderedResults = orderByDate(results);
        return new ProbeResultEntry(entryResultType(mostRecentSuccess(dateOrderedResults), dateOrderedResults.get(0), day, type), filename, buildEntryValue(results));
    }

    private FileUploadResult mostRecentSuccess(List<? extends FileUploadResult> results) {
        return Iterables.get(Iterables.filter(results, IS_REMOTE_SUCCESS), 0, null);
    }

    private List<? extends FileUploadResult> orderByDate(Iterable<? extends FileUploadResult> results) {
        return DATE_ORDERING.reverse().immutableSortedCopy(results);
    }

    private String linkedFilename(FileType type, LocalDate day) {
        return String.format("<a style=\"text-decoration:none\" href=\"/feeds/ukradioplayer/%2$s_%3$s_%4$s.xml\">%2$s_%3$s_%4$s.xml</a>", day.toString("yyyyMMdd"), service.getRadioplayerId(), type.name());
    }

    private ProbeResultType entryResultType(FileUploadResult mostRecentSuccess, FileUploadResult mostRecent, LocalDate day, FileType type) {
        if (mostRecentSuccess != null) {
            if (FileUploadResultType.SUCCESS.equals(mostRecent.remoteProcessingResult()) && FileType.OD == type) {
                return SUCCESS;
            }
            if (!isStale(mostRecentSuccess)) {
                if (FileUploadResultType.SUCCESS.equals(mostRecent.remoteProcessingResult())) {
                    return probeResultTypeFrom(mostRecent, day, type);
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
        return probeResultTypeFrom(mostRecent, day, type);
    }

    private boolean isStale(FileUploadResult mostRecent) {
        return olderThan(mostRecent, FAILURE_WINDOW);
    }

    private ProbeResultType probeResultTypeFrom(FileUploadResult result, LocalDate day, FileType type) {
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

    protected String buildEntryValue(Iterable<? extends FileUploadResult> results) {
        StringBuilder builder = new StringBuilder("<table>");
        for (FileUploadResult result : Iterables.limit(results,2)) {
            appendResult(builder, result);
        }
        return builder.append("</table>").toString();
    }

    private void appendResult(StringBuilder builder, FileUploadResult result) {
        builder.append("<tr><td>Last ");
        builder.append(result.type().toNiceString());
        builder.append(": ");
        builder.append(result.uploadTime().toString(DATE_TIME));
        builder.append("</td><td>");
        if (result.transactionId() != null) {
            builder.append("Transaction status url: " + result.transactionId());
            if (result.message() != null) {
                builder.append(" " + result.message());
            }
        } else if (result.message() != null) {
            builder.append(result.message());
        } else {
            if (FileUploadResultType.SUCCESS == result.type()) {
                builder.append("File uploaded successfully");
            } else if (result.exceptionSummary() != null && result.exceptionSummary().message() != null) {
                builder.append(result.exceptionSummary().message());
            }
        }
        builder.append("</td><td>");
        FileUploadResultType processSuccess = result.remoteProcessingResult() == null ? FileUploadResultType.UNKNOWN : result.remoteProcessingResult();
        builder.append("Processing Result: " + processSuccess.toNiceString());
        builder.append("</td></tr>");
    }
    
    private String uploadButton(FileType type, LocalDate day) {
        String postTarget = String.format("/feeds/ukradioplayer/upload/%s/%s/%s", remoteServiceId, type.name(), service.getRadioplayerId());
        if(day != null) {
            postTarget += day.toString("/yyyyMMdd");
        }
        return "<form style=\"text-align:center\" action=\""+postTarget+"\" method=\"post\"><input type=\"submit\" value=\"Update\"/></form>";
    }

    private ProbeResultEntry uploadAllPi() {
        return new ProbeResultEntry(INFO, "Update All PI files", uploadButton(PI, null));
    }

    @Override
    public String title() {
        return service.getName();
    }

    @Override
    public String slug() {
        return String.format("ukrp-%s-%s", remoteServiceId, service.getName());
    }

}
