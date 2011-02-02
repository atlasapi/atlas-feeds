package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;
import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType.UNKNOWN;

import java.util.List;

import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.mongodb.DBObject;

public class RadioPlayerServerHealthProbe extends RadioPlayerUploadHealthProbe {

    public RadioPlayerServerHealthProbe(DatabasedMongo mongo, String serviceName, String serviceId, DayRangeGenerator dayRangeGenerator) {
        super(mongo, serviceName, serviceId, dayRangeGenerator);
    }

    @Override
    public ProbeResult probe() {
        ProbeResult result = new ProbeResult(title());

        result.addEntry(buildEntry());

        return result;
    }

    private ProbeResultEntry buildEntry() {
        Iterable<RadioPlayerFTPUploadResult> fileResults = Iterables.filter(Iterables.transform(RESULT_TYPES, new Function<FTPUploadResultType, RadioPlayerFTPUploadResult>() {
            @Override
            public RadioPlayerFTPUploadResult apply(FTPUploadResultType input) {
                DBObject dboResult = results.findOne(id(input, serviceId));
                if(dboResult != null) {
                    return translator.fromDBObject(dboResult);
                }
                return null;
            }
        }), Predicates.notNull());
        return entryFor(TYPE_ORDERING.immutableSortedCopy(fileResults));
    }
    
    private String id(FTPUploadResultType type, String serviceId) {
        return String.format("%s:%s:", type, serviceId);
    }
    
    private ProbeResultEntry entryFor(List<? extends FTPUploadResult> results) {
        String filename = serviceId;
        if(results.isEmpty()) {
            return new ProbeResultEntry(INFO, filename, "No Data.");
        }
        String value = buildValue(results);
        FTPUploadResultType first = DATE_ORDERING.immutableSortedCopy(results).get(0).type();
        if(UNKNOWN.equals(first)) {
            return new ProbeResultEntry(INFO, filename, value);
        } else {
            return new ProbeResultEntry(FTPUploadResultType.SUCCESS.equals(first) ? SUCCESS : FAILURE, filename, value);
        }
    }

}
