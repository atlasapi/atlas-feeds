package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.DATE_ORDERING;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.TYPE_ORDERING;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.RESULT_TYPES;

import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;
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

        for (LocalDate day : rangeGenerator.generate(new LocalDate(DateTimeZones.UTC))) {
            result.addEntry(entryFor(day));
        }

        return result;
    }

    private ProbeResultEntry entryFor(final LocalDate day) {
        Iterable<RadioPlayerFTPUploadResult> fileResults = Iterables.filter(Iterables.transform(RESULT_TYPES, new Function<FTPUploadResultType, RadioPlayerFTPUploadResult>() {
            @Override
            public RadioPlayerFTPUploadResult apply(FTPUploadResultType input) {
                DBObject dboResult = results.findOne(id(input, service.getRadioplayerId(), day));
                if(dboResult != null) {
                    return translator.fromDBObject(dboResult);
                }
                return null;
            }
        }), Predicates.notNull());
        return entryFor(day, fileResults);
    }
    
    private String id(FTPUploadResultType type, int serviceId, LocalDate day) {
        return String.format("%s:%s:%s", type, serviceId, day.toString("yyyyMMdd"));
    }
    
    private ProbeResultEntry entryFor(LocalDate day, Iterable<? extends FTPUploadResult> results) {
        String filename = String.format("%s_%s_PI.xml", day.toString("yyyyMMdd"), service.getRadioplayerId());
        if(Iterables.isEmpty(results)) {
            return new ProbeResultEntry(INFO, filename, "No Data");
        }
        String value = buildValue(TYPE_ORDERING.immutableSortedCopy(results));
        
        FTPUploadResult mostRecent = DATE_ORDERING.reverse().immutableSortedCopy(results).get(0);
        return new ProbeResultEntry(resultType(mostRecent, day), filename, value);
    }
    
    private ProbeResultType resultType(FTPUploadResult mostRecent, LocalDate day) {
        switch (mostRecent.type()) {
        case UNKNOWN:
            return INFO;
        case SUCCESS:
            if(mostRecent.uploadTime().plusMinutes(20).isBeforeNow()) {
                return FAILURE;
            }
            return SUCCESS;
        case FAILURE:
            if(day.isAfter(mostRecent.uploadTime().toLocalDate()) || service.getName().equals("5livesportsextra")) {
                return INFO;
            } else {
                return FAILURE;
            }
        default:
            return INFO;
        }
    }

    protected String buildValue(List<? extends FTPUploadResult> results) {
        StringBuilder builder = new StringBuilder("<table>");
        for(FTPUploadResult result : Iterables.limit(results,2)) {
            builder.append("<tr><td>Last ");
            builder.append(result.type().toNiceString());
            builder.append(": ");
            builder.append(result.uploadTime().toString(DATE_TIME));
            builder.append("</td><td>");
            if(result.message() != null) {
                builder.append(result.message());
            }
            builder.append("</td></tr>");
        }
        return builder.append("</table>").toString();
    }
    
    @Override
    public String title() {
        return service.getName();
    }

    @Override
    public String slug() {
        return "ukrp"+service.getName();
    }

}
