package org.atlasapi.feeds.radioplayer.upload.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.radioplayer.upload.persistence.UploadTaskTranslator.TIMESTAMP_KEY;

import org.atlasapi.feeds.radioplayer.upload.queue.MongoTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.QueueTask;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.mongo.MongoSortBuilder;
import com.metabroadcast.common.time.Clock;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MongoTaskQueue<T extends QueueTask> implements TaskQueue<T> {
    
    private final DBCollection collection;
    private final MongoTranslator<T> translator;
    private final Clock clock;
    
    public MongoTaskQueue(DatabasedMongo mongo, String queueName, MongoTranslator<T> translator, Clock clock) {
        this.collection = checkNotNull(mongo).collection(checkNotNull(queueName));
        this.translator = checkNotNull(translator);
        this.clock = checkNotNull(clock);
    }

    @Override
    public void push(T task) {
        if (task.timestamp() == null) {
            task.setTimestamp(clock.now());
        }
        collection.save(translator.toDBObject(task));
    }

    @Override
    public Optional<T> fetchOne() {
        return Optional.fromNullable(translator.fromDBObject(
                Iterables.getFirst(
                        collection.find().sort(
                                new MongoSortBuilder()
                                .ascending(TIMESTAMP_KEY)
                                .build()
                        ), 
                        null
                )
        ));
    }


    @Override
    public boolean remove(T task) {
        DBObject dbo = collection.findAndRemove(new MongoQueryBuilder().idEquals(task.file().toKey()).build());
        return dbo != null;
    }

}
