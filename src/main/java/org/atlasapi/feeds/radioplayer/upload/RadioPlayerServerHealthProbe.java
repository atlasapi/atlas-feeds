package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.DATE_ORDERING;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.TYPE_ORDERING;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.RESULT_TYPES;

import java.util.List;

import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class RadioPlayerServerHealthProbe implements HealthProbe {

    private final DBCollection results;
    private final FTPCredentials credentials;
    private final FTPUploadResultTranslator translator;

    public RadioPlayerServerHealthProbe(DatabasedMongo mongo, FTPCredentials credentials) {
        this.results = mongo.collection("radioplayer");
        this.credentials = credentials;
        this.translator = new FTPUploadResultTranslator();
    }

    @Override
    public ProbeResult probe() {
        return new ProbeResult(title()).addEntry(buildEntry());
    }

    private ProbeResultEntry buildEntry() {
        Iterable<FTPUploadResult> fileResults = Iterables.filter(Iterables.transform(RESULT_TYPES, new Function<FTPUploadResultType, FTPUploadResult>() {
            @Override
            public FTPUploadResult apply(FTPUploadResultType input) {
                DBObject dboResult = results.findOne(id(input));
                if(dboResult != null) {
                    return translator.fromDBObject(dboResult);
                }
                return null;
            }
        }), Predicates.notNull());
        return entryFor(fileResults);
    }
    
    private String id(FTPUploadResultType type) {
        return String.format("%s:%s", type, credentials.toString());
    }
    
    private ProbeResultEntry entryFor(Iterable<? extends FTPUploadResult> results) {
        if(Iterables.isEmpty(results)) {
            return new ProbeResultEntry(INFO, credentials.toString(), "No Data");
        }
        
        String value = buildValue(TYPE_ORDERING.immutableSortedCopy(results));
        
        FTPUploadResult mostRecent = DATE_ORDERING.reverse().immutableSortedCopy(results).get(0);
        return new ProbeResultEntry(resultType(mostRecent), credentials.toString(), value);
    }
    
    private ProbeResultType resultType(FTPUploadResult mostRecent) {
        switch (mostRecent.type()) {
            case UNKNOWN:
                return INFO;
            case SUCCESS:
                if (mostRecent.uploadTime().plusMinutes(20).isBeforeNow()) {
                    return FAILURE;
                }
                return SUCCESS;
            case FAILURE:
                return FAILURE;
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
            builder.append(result.uploadTime().toString("dd/MM/yy HH:mm:ss"));
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
        return "FTP";
    }

    @Override
    public String slug() {
        return "ukrpFTP";
    }

}
