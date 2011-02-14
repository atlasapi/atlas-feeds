package org.atlasapi.feeds.radioplayer.upload;

import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoFTPUploadResultStore implements RadioPlayerFTPUploadResultStore {

    private final DBCollection recordings;
    private final RadioPlayerFTPUploadResultTranslator translator;

    public MongoFTPUploadResultStore(DatabasedMongo mongo) {
        this.recordings = mongo.collection("radioplayer");
        this.translator = new RadioPlayerFTPUploadResultTranslator();
    }

    @Override
    public void record(RadioPlayerFTPUploadResult result) {
        this.recordings.save(translator.toDBObject(result));
    }

    @Override
    public Set<RadioPlayerFTPUploadResult> resultsFor(RadioPlayerService service, LocalDate day) {
        return ImmutableSet.copyOf(Iterables.transform(recordings.find(queryFor(service,day)), new Function<DBObject, RadioPlayerFTPUploadResult>() {

            @Override
            public RadioPlayerFTPUploadResult apply(DBObject input) {
                return translator.fromDBObject(input);
            }

        }
        ));
    }

    private DBObject queryFor(RadioPlayerService service, LocalDate day) {
        BasicDBObject dbo = new BasicDBObject("serviceId", service.getRadioplayerId());
        TranslatorUtils.fromLocalDate(dbo, "day", day);
        return dbo;
    }
}
