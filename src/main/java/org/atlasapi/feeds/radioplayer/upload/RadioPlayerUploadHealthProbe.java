package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.webapp.health.HealthProbe;
import com.metabroadcast.common.webapp.health.ProbeResult;
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
            for (int i = 0; i < (lookAhead+lookBack+1); i++, day = day.plusDays(1)) {
                RadioPlayerUploadResult success = resultFor(service, day, true);
                RadioPlayerUploadResult failure = resultFor(service, day, false);
                String key = service.getName()+" "+day.toString("dd/MM/yyyy");
                addResult(result, success, failure, key);
            }
        }
        
        return result;
    }

    private void addResult(ProbeResult result, RadioPlayerUploadResult success, RadioPlayerUploadResult failure, String key) {
        if(success == null && failure == null) {
            result.addInfo(key, "No Data.");
            return;
        }
        if(success != null) {
            if(failure != null) {
                String value = String.format("Last success %s. Last failure %s.", success.uploadTime().toString(DATE_TIME), failure.uploadTime().toString(DATE_TIME));
                if(success.uploadTime().isAfter(failure.uploadTime())) {
                    result.add(key, value, true);
                } else {
                    result.add(key, String.format("%s %s",value, failure.message()), false);
                }
                return;
            }
            result.add(key, String.format("Last success %s. No failures.", success.uploadTime().toString(DATE_TIME)), true);
            return;
        }
        if(failure != null) {
            result.add(key, String.format("No successes. Last failure %s. %s", failure.uploadTime().toString(DATE_TIME), failure.message()), false);
        }
    }

    private RadioPlayerUploadResult resultFor(RadioPlayerService service, DateTime day, boolean success) {
        DBCursor latestSuccess = results.find(new BasicDBObject("filename",filenameFor(service,day)).append("success", success)).sort(new BasicDBObject("time",-1)).limit(1);
        DBObject first = Iterables.getFirst(latestSuccess, null);
        if(first != null) {
            return translator.fromDBObject(first);
        }
        return null;
    }

    private String filenameFor(RadioPlayerService service, DateTime day) {
        return String.format("%s_%s_PI.xml", day.toString(DATE), service.getRadioplayerId());
    }

    @Override
    public String title() {
        return "UK Radioplayer";
    }

}
