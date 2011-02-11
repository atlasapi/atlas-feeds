package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;

import java.util.List;
import java.util.Map;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class RadioPlayerUploadHealthProbe implements HealthProbe {

    protected static final String DATE_TIME = "dd/MM/yy HH:mm:ss";

    protected final DBCollection results;
    private final RadioPlayerService service;
    protected final DayRangeGenerator rangeGenerator;
    protected final RadioPlayerFTPUploadResultTranslator translator;

    public RadioPlayerUploadHealthProbe(DatabasedMongo mongo, RadioPlayerService service, DayRangeGenerator dayRangeGenerator) {
        this.results = mongo.collection("radioplayer");
        this.service = service;
        this.rangeGenerator = dayRangeGenerator;
        this.translator = new RadioPlayerFTPUploadResultTranslator();
    }

    @Override
    public ProbeResult probe() {
        ProbeResult result = new ProbeResult(service.getName());
        DayRange dayRange = rangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
        
        List<RadioPlayerFTPUploadResult> results = resultsBetween(Iterables.get(dayRange, 0), Iterables.getLast(dayRange));
        
        Map<LocalDate, RadioPlayerFTPUploadResult> successes = Maps.newHashMapWithExpectedSize(Iterables.size(dayRange));
        Map<LocalDate, RadioPlayerFTPUploadResult> failures = Maps.newHashMapWithExpectedSize(Iterables.size(dayRange));
        
        for (RadioPlayerFTPUploadResult uploadResult : results) {
            if(uploadResult.type() == FTPUploadResultType.SUCCESS) {
                successes.put(uploadResult.day(), uploadResult);
            } else if (uploadResult.type() == FTPUploadResultType.FAILURE) {
                failures.put(uploadResult.day(), uploadResult);
            }
        }
        
        for (LocalDate day : dayRange) {
            result.addEntry(entryFor(day, successes.get(day),failures.get(day)));
        }

        result.addEntry(uploadAll());

        return result;
    }

    private List<RadioPlayerFTPUploadResult> resultsBetween(LocalDate first, LocalDate last) {
        return ImmutableList.copyOf(Iterables.transform(results.find(queryFor(first, last)), new Function<DBObject, RadioPlayerFTPUploadResult>() {
            @Override
            public RadioPlayerFTPUploadResult apply(DBObject dboResult) {
                return translator.fromDBObject(dboResult);
            }
        }));
    }

    private DBObject queryFor(LocalDate first, LocalDate last) {
        DBObject query = new BasicDBObject("serviceId", service.getRadioplayerId());
        DBObject dayConstraint = new BasicDBObject();
        TranslatorUtils.fromLocalDate(dayConstraint, MongoConstants.GREATER_THAN_OR_EQUAL_TO, first);
        TranslatorUtils.fromLocalDate(dayConstraint, MongoConstants.LESS_THAN_OR_EQUAL_TO, last);
        query.put("day", dayConstraint);
        return query;
    }

    private ProbeResultEntry entryFor(LocalDate day, FTPUploadResult success, FTPUploadResult failure) {
        String filename = linkedFilename(day) + uploadButton(day);
        
        if (success == null && failure == null) {
            return new ProbeResultEntry(INFO, filename, "No Data");
        }
        
        return new ProbeResultEntry(entryResultType(mostRecentResult(success, failure), day), filename, buildEntryValue(success, failure));
    }

    private FTPUploadResult mostRecentResult(FTPUploadResult success, FTPUploadResult failure) {
        if (success == null) {
            return failure;
        }
        if (failure == null) {
            return success;
        }
        return success.uploadTime().isAfter(failure.uploadTime()) ? success : failure;
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

    protected String buildEntryValue(FTPUploadResult success, FTPUploadResult failure) {
        StringBuilder builder = new StringBuilder("<table>");
        if(success != null) {
            appendResult(builder, success);
        }
        if(failure != null) {
            appendResult(builder, failure);
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
