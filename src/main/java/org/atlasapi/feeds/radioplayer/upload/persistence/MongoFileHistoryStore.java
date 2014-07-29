package org.atlasapi.feeds.radioplayer.upload.persistence;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;

import com.google.common.base.Optional;
import com.metabroadcast.common.ids.IdGenerator;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.mongodb.DBCollection;


public class MongoFileHistoryStore implements FileHistoryStore {
    
    private final DBCollection collection;
    private final IdGenerator generator;
    private final FileHistoryTranslator translator;
    
    public MongoFileHistoryStore(DatabasedMongo mongo, IdGenerator generator) {
        this.collection = checkNotNull(mongo).collection("radioPlayerFiles");
        this.generator = checkNotNull(generator);
        this.translator = new FileHistoryTranslator();
    }

    @Override
    public void store(FileHistory history) {
        collection.save(translator.toDBObject(history));
    }

    @Override
    public Optional<FileHistory> fetch(RadioPlayerFile file) {
        return Optional.fromNullable(translator.fromDBObject(collection.findOne(
                new MongoQueryBuilder()
                    .idEquals(file.toKey())
                    .build()
        )));
    }
    
    @Override
    public synchronized UploadAttempt addUploadAttempt(RadioPlayerFile file, UploadAttempt attempt) {
        Optional<FileHistory> fetched = fetch(file);
        if (!fetched.isPresent()) {
            throw new RuntimeException("Attempting to add upload attempt to non-existent file record " + file.toString());
        }
        UploadAttempt withId = attempt.copyWithId(generator.generateRaw());
        fetched.get().addUploadAttempt(withId);
        store(fetched.get());

        return withId;
    }

    @Override
    public synchronized void successfulUpload(RadioPlayerFile file) {
        Optional<FileHistory> fetched = fetch(file);
        if (!fetched.isPresent()) {
            throw new RuntimeException("Attempting to change queuing flags on non-existent file record " + file.toString());
        }
        fetched.get().setEnqueuedForUpload(false);
        fetched.get().setEnqueuedForRemoteCheck(true);
        
        store(fetched.get());
    }

}
