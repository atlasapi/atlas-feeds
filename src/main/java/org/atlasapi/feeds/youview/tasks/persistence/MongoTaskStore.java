package org.atlasapi.feeds.youview.tasks.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.CONTENT_KEY;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.PUBLISHER_KEY;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.REMOTE_ID_KEY;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.STATUS_KEY;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.UPLOAD_TIME_KEY;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.tasks.persistence.TaskTranslator.toDBObject;

import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.TaskQuery;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.mongo.MongoSortBuilder;
import com.metabroadcast.common.persistence.mongo.MongoUpdateBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;


public class MongoTaskStore implements TaskStore {
    
    private static final String COLLECTION_NAME = "youviewTasks";
    
    private final DBCollection collection;
    
    public MongoTaskStore(DatabasedMongo mongo) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
    }

    private DBCursor getOrderedCursor(DBObject query) {
        return collection.find(query).sort(new MongoSortBuilder().descending(UPLOAD_TIME_KEY).build());
    }

    @Override
    public Task save(Task task) {
        collection.save(toDBObject(task));
        return task;
    }

    @Override
    public boolean updateWithStatus(Long taskId, Status status) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        DBObject updateStatus = new MongoUpdateBuilder()
                .setField(STATUS_KEY, status.name())
                .build();
        
        WriteResult result = collection.update(idQuery, updateStatus, false, false);
        return result.getN() == 1;
    }

    @Override
    public boolean updateWithRemoteId(Long taskId, Status status, String remoteId, DateTime uploadTime) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        DBObject updateStatus = new MongoUpdateBuilder()
                .setField(TaskTranslator.STATUS_KEY, status.name())
                .setField(TaskTranslator.REMOTE_ID_KEY, remoteId)
                .setField(TaskTranslator.UPLOAD_TIME_KEY, uploadTime)
                .build();
        
        WriteResult result = collection.update(idQuery, updateStatus, false, false);
        return result.getN() == 1;
    }

    @Override
    public boolean updateWithResponse(Long taskId, Response response) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        DBObject updateStatus = new MongoUpdateBuilder()
                .push(TaskTranslator.REMOTE_STATUSES_KEY, ResponseTranslator.toDBObject(response))
                .setField(STATUS_KEY, response.status().name())
                .build();
        
        WriteResult result = collection.update(idQuery, updateStatus, false, false);
        return result.getN() == 1;
    }

    @Override
    public Optional<Task> taskFor(Long taskId) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        return Optional.fromNullable(fromDBObject(collection.findOne(idQuery)));
    }

    @Override
    public Iterable<Task> allTasks(Status status) {
        MongoQueryBuilder mongoQuery = new MongoQueryBuilder()
                .fieldEquals(STATUS_KEY, status.name());
        
        DBCursor cursor = getOrderedCursor(mongoQuery.build());
        
        return FluentIterable.from(cursor)
                .transform(TaskTranslator.fromDBObjects())
                .filter(Predicates.notNull());
    }

    @Override
    public Iterable<Task> allTasks(TaskQuery query) {
        MongoQueryBuilder mongoQuery = new MongoQueryBuilder();
        
        mongoQuery.fieldEquals(PUBLISHER_KEY, query.publisher().key());
        
        // TODO regex matching
        if (query.contentUri().isPresent()) {
            mongoQuery.regexMatch(CONTENT_KEY, transformToRegexPattern(query.contentUri().get()));
        }
        if (query.transactionId().isPresent()) {
            mongoQuery.regexMatch(REMOTE_ID_KEY, transformToRegexPattern(query.transactionId().get()));
        }
        if (query.status().isPresent()) {
            mongoQuery.fieldEquals(STATUS_KEY, query.status().get().name());
        }
        
        DBCursor cursor = getOrderedCursor(mongoQuery.build())
                .skip(query.selection().getOffset())
                .limit(query.selection().getLimit());
        
        return FluentIterable.from(cursor)
                .transform(TaskTranslator.fromDBObjects())
                .filter(Predicates.notNull());
    }

    private String transformToRegexPattern(String input) {
//        return "/" + input.replace(".", "\\.") + "/";
        return input.replace(".", "\\.");
    }
}
