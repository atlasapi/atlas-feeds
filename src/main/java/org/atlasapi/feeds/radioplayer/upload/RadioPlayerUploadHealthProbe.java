package org.atlasapi.feeds.radioplayer.upload;

import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DateTimeZones;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class RadioPlayerUploadHealthProbe implements HealthProbe {

    private static final String DATE = "yyyyMMdd";
    private static final String DATE_TIME = "dd/MM/yy HH:mm:ss";

    private DBCollection results;
    private RadioPlayerUploadResultTranslator translator;
    private final Iterable<RadioPlayerService> services;
    private int lookBack = 2;
    private int lookAhead = 7;

    public RadioPlayerUploadHealthProbe(DatabasedMongo mongo, Iterable<RadioPlayerService> services) {
        this.services = services;
        this.results = mongo.collection("radioplayer");
        this.translator = new RadioPlayerUploadResultTranslator();
    }

    public RadioPlayerUploadHealthProbe withLookBack(int lookBack) {
        this.lookBack = lookBack;
        return this;
    }

    public RadioPlayerUploadHealthProbe withLookAhead(int lookAhead) {
        this.lookAhead = lookAhead;
        return this;
    }

    @Override
    public ProbeResult probe() {
        ProbeResult result = new ProbeResult("UK Radioplayer");

        DateTime day = new LocalDate().toInterval(DateTimeZones.UTC).getStart().minusDays(lookBack);
        for (RadioPlayerService service : services) {
            for (int i = 0; i < (lookAhead + lookBack + 1); i++, day = day.plusDays(1)) {
                addEntry(result, service, day);
            }
        }

        return result;
    }

    private void addEntry(ProbeResult result, RadioPlayerService service, DateTime day) {
        boolean success = true, failure = true, unknown = true;
        DBCursor resultsForServiceDay = results.find(new BasicDBObject("filename", filenameFor(service, day))).sort(new BasicDBObject("time", -1));
        List<RadioPlayerUploadResult> results = Lists.newArrayList();
        for (DBObject dbo : resultsForServiceDay) {
            RadioPlayerUploadResult translated = translator.fromDBObject(dbo);
            if (translated.wasSuccessful() == null) { 
                if(unknown) {
                    results.add(translated);
                    unknown = false;
                }
            } else if (translated.wasSuccessful() && success) {
                results.add(translated);
                success = false;
            } else if (!translated.wasSuccessful() && failure) {
                results.add(translated);
                failure = false;
            }
            if (!success && !failure && !unknown) {
                break;
            }
        }
        addEntry(result, String.format("%s %s", service.getName(), day.toString("dd/MM/yyyy")), results);
    }

    private void addEntry(ProbeResult result, String key, List<RadioPlayerUploadResult> results) {
        if (results.isEmpty()) {
            result.addInfo(key, "No Data.");
            return;
        }
        String value = buildValue(results);
        Boolean first = results.get(0).wasSuccessful();
        if (first == null) {
            result.addInfo(key, value);
        } else {
            result.add(key, value, first);
        }
    }

    private String buildValue(List<RadioPlayerUploadResult> results) {
        StringBuilder builder = new StringBuilder("<table>");
        for (RadioPlayerUploadResult result : results) {
            builder.append("<tr><td>");
            if (result.wasSuccessful() == null) {
                builder.append("Unknown");
            } else {
                builder.append(result.wasSuccessful() ? "Success :" : "Failure :");
            }
            builder.append(result.uploadTime().toString(DATE_TIME));
            builder.append("</td><td>");
            if(result.message() != null) {
                builder.append(result.message());
            }
            builder.append("</td></tr>");
        }
        return builder.append("</table>").toString();
    }

    private String filenameFor(RadioPlayerService service, DateTime day) {
        return String.format("%s_%s_PI.xml", day.toString(DATE), service.getRadioplayerId());
    }

    @Override
    public String title() {
        return "UK Radioplayer";
    }

}
