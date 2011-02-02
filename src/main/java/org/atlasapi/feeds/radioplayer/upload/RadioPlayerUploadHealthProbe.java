package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;

import java.util.Comparator;
import java.util.List;

import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class RadioPlayerUploadHealthProbe implements HealthProbe {

    protected static final Ordering<FTPUploadResult> DATE_ORDERING = Ordering.from(new Comparator<FTPUploadResult>() {
        @Override
        public int compare(FTPUploadResult r1, FTPUploadResult r2) {
            return r2.uploadTime().compareTo(r1.uploadTime());
        }
    });

    protected static final Ordering<FTPUploadResult> TYPE_ORDERING = Ordering.from(new Comparator<FTPUploadResult>() {
        @Override
        public int compare(FTPUploadResult r1, FTPUploadResult r2) {
            return r1.type().compareTo(r2.type());
        }
    });

    protected static final ImmutableList<FTPUploadResultType> RESULT_TYPES = ImmutableList.copyOf(FTPUploadResultType.values());
    protected static final String DATE_TIME = "dd/MM/yy HH:mm:ss";

    protected final DBCollection results;
    protected final RadioPlayerFTPUploadResultTranslator translator;
    protected final String serviceName;
    protected final String serviceId;
    protected final DayRangeGenerator rangeGenerator;
    
    public RadioPlayerUploadHealthProbe(DatabasedMongo mongo, String serviceName, String serviceId, DayRangeGenerator dayRangeGenerator) {
        this.results = mongo.collection("radioplayer");
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.rangeGenerator = dayRangeGenerator;
        this.translator = new RadioPlayerFTPUploadResultTranslator();
    }

    @Override
    public ProbeResult probe() {
        ProbeResult result = new ProbeResult(serviceName);

        for (LocalDate day : rangeGenerator.generate(new LocalDate(DateTimeZones.UTC))) {
            result.addEntry(entryFor(day));
        }

        return result;
    }

    private ProbeResultEntry entryFor(final LocalDate day) {
        Iterable<RadioPlayerFTPUploadResult> fileResults = Iterables.filter(Iterables.transform(RESULT_TYPES, new Function<FTPUploadResultType, RadioPlayerFTPUploadResult>() {
            @Override
            public RadioPlayerFTPUploadResult apply(FTPUploadResultType input) {
                DBObject dboResult = results.findOne(id(input, serviceId, day));
                if(dboResult != null) {
                    return translator.fromDBObject(dboResult);
                }
                return null;
            }
        }), Predicates.notNull());
        return entryFor(day, TYPE_ORDERING.immutableSortedCopy(fileResults));
    }
    
    private String id(FTPUploadResultType type, String serviceId, LocalDate day) {
        return String.format("%s:%s:%s", type, serviceId, day.toString("yyyyMMdd"));
    }
    
    private ProbeResultEntry entryFor(LocalDate day, List<? extends FTPUploadResult> results) {
        String filename = filename(day);
        if(results.isEmpty()) {
            return new ProbeResultEntry(INFO, filename, "No Data.");
        }
        String value = buildValue(results);
        FTPUploadResultType first = DATE_ORDERING.immutableSortedCopy(results).get(0).type();
        switch (first) {
        case UNKNOWN:
            return new ProbeResultEntry(INFO, filename, value);
        case SUCCESS:
            return new ProbeResultEntry(SUCCESS, filename, value);
        case FAILURE:
            if(day.isAfter(new LocalDate(DateTimeZones.UTC)) || serviceName.equals("5livesportsextra")) {
                return new ProbeResultEntry(INFO, filename, value);
            } else {
                return new ProbeResultEntry(FAILURE, filename, value);
            }
        default:
            return new ProbeResultEntry(INFO, filename, value);
        }
    }

    private String filename(LocalDate day) {
        return String.format("%s_%s_PI.xml", day.toString("yyyyMMdd"), serviceId);
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
        return serviceName;
    }

    @Override
    public String slug() {
        return "ukrp"+serviceName;
    }

}
