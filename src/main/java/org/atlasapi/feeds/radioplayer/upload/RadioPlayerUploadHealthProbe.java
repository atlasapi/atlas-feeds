package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.DATE_ORDERING;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.joda.time.LocalDate;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadHealthProbe implements HealthProbe {

    protected static final String DATE_TIME = "dd/MM/yy HH:mm:ss";

    protected final RadioPlayerFTPUploadResultStore store;
    private final RadioPlayerService service;
    protected final DayRangeGenerator rangeGenerator;
    protected final RadioPlayerFTPUploadResultTranslator translator;

    public RadioPlayerUploadHealthProbe(RadioPlayerFTPUploadResultStore store, RadioPlayerService service, DayRangeGenerator dayRangeGenerator) {
        this.store = store;
        this.service = service;
        this.rangeGenerator = dayRangeGenerator;
        this.translator = new RadioPlayerFTPUploadResultTranslator();
    }

    @Override
    public ProbeResult probe() {
        ProbeResult result = new ProbeResult(service.getName());
        
        DayRange dayRange = rangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
        
        for (LocalDate day : dayRange) {
            result.addEntry(entryFor(day, store.resultsFor(service, day)));
        }

        result.addEntry(uploadAll());

        return result;
    }

    private ProbeResultEntry entryFor(LocalDate day, Iterable<? extends FTPUploadResult> results) {
        String filename = linkedFilename(day) + uploadButton(day);
        
        if (Iterables.isEmpty(results)) {
            return new ProbeResultEntry(INFO, filename, "No Data");
        }
        
        return new ProbeResultEntry(entryResultType(mostRecentResult(results), day), filename, buildEntryValue(results));
    }

    private FTPUploadResult mostRecentResult(Iterable<? extends FTPUploadResult> results) {
        return DATE_ORDERING.reverse().immutableSortedCopy(results).get(0);
    }

    private String linkedFilename(LocalDate day) {
        return String.format("<a style=\"text-decoration:none\" href=\"/feeds/ukradioplayer/%1$s_%2$s_PI.xml\">%1$s_%2$s_PI.xml</a>", day.toString("yyyyMMdd"), service.getRadioplayerId());
    }

    private ProbeResultType entryResultType(FTPUploadResult mostRecent, LocalDate day) {
        switch (mostRecent.type()) {
        case SUCCESS:
            if (day.isEqual(new LocalDate(DateTimeZones.UTC)) && mostRecent.uploadTime().plusMinutes(20).isBeforeNow()) {
                return FAILURE;
            }
            if (mostRecent.uploadTime().plusHours(4).isBeforeNow()) {
                return FAILURE;
            }
            return SUCCESS;
        case FAILURE:
            if (day.isAfter(mostRecent.uploadTime().toLocalDate().plusDays(1)) || service.getName().equals("5livesportsextra")) {
                return INFO;
            } else {
                return FAILURE;
            }
        default:
            return INFO;
        }
    }

    protected String buildEntryValue(Iterable<? extends FTPUploadResult> results) {
        StringBuilder builder = new StringBuilder("<table>");
        for (FTPUploadResult result : Iterables.limit(FTPUploadResult.TYPE_ORDERING.immutableSortedCopy(results),2)) {
            appendResult(builder, result);
        }
        return builder.append("</table>").toString();
    }

    private void appendResult(StringBuilder builder, FTPUploadResult result) {
        builder.append("<tr><td>Last ");
        builder.append(result.type().toNiceString());
        builder.append(": ");
        builder.append(result.uploadTime().toString(DATE_TIME));
        builder.append("</td><td>");
        if (result.message() != null) {
            builder.append(result.message());
        }
        builder.append("</td></tr>");
    }

    private String uploadButton(LocalDate day) {
        String postTarget = String.format("/feeds/ukradioplayer/upload/%s", service.getRadioplayerId());
        if(day != null) {
            postTarget += day.toString("/yyyyMMdd");
        }
        return "<form style=\"text-align:center\" action=\""+postTarget+"\" method=\"post\"><input type=\"submit\" value=\"Update\"/></form>";
    }

    private ProbeResultEntry uploadAll() {
        return new ProbeResultEntry(INFO, "Update All", uploadButton(null));
    }

    @Override
    public String title() {
        return service.getName();
    }

    @Override
    public String slug() {
        return "ukrp" + service.getName();
    }

}
