package org.atlasapi.feeds.radioplayer.health;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadResultStore;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.LocalDate;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadHealthProbe implements HealthProbe {

    protected static final String DATE_TIME = "dd/MM/yy HH:mm:ss";
    
    protected final RadioPlayerUploadResultStore store;
    protected final DayRangeGenerator rangeGenerator;
    
    private final String remoteServiceId;
    private final RadioPlayerService service;
    private final Clock clock;
    private final StateChecker stateChecker;

    public RadioPlayerUploadHealthProbe(Clock clock, String remoteServiceId, RadioPlayerUploadResultStore store, RadioPlayerService service, DayRangeGenerator dayRangeGenerator, StateChecker stateChecker) {
        this.clock = clock;
        this.remoteServiceId = remoteServiceId;
        this.store = store;
        this.service = service;
        this.rangeGenerator = dayRangeGenerator;
        this.stateChecker = stateChecker;
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
        List<? extends FileUploadResult> dateOrderedResults = stateChecker.orderByDate(results);
        return new ProbeResultEntry(stateChecker.entryResultType(stateChecker.mostRecentSuccess(dateOrderedResults), dateOrderedResults.get(0), day, type, service), filename, buildEntryValue(results));
    }

    private String linkedFilename(FileType type, LocalDate day) {
        return String.format("<a style=\"text-decoration:none\" href=\"/feeds/ukradioplayer/%1$s_%2$s_%3$s.xml\">%1$s_%2$s_%3$s.xml</a>", day.toString("yyyyMMdd"), service.getRadioplayerId(), type.name());
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
