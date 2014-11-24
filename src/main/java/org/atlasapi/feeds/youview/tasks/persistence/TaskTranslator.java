package org.atlasapi.feeds.youview.tasks.persistence;

import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class TaskTranslator {
    
    static final String PUBLISHER_KEY = "publisher";
    static final String ACTION_KEY = "action";
    static final String CONTENT_KEY = "contentUrls";
    static final String UPLOAD_TIME_KEY = "uploadTime";
    static final String REMOTE_ID_KEY = "remoteId";
    static final String REMOTE_STATUSES_KEY = "remoteStatuses";
    static final String STATUS_KEY = "status";
    
    public static DBObject toDBObject(Task task) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, task.id());
        TranslatorUtils.from(dbo, PUBLISHER_KEY, task.publisher().key());
        TranslatorUtils.from(dbo, ACTION_KEY, task.action().name());
        TranslatorUtils.from(dbo, CONTENT_KEY, task.content());
        TranslatorUtils.fromDateTime(dbo, UPLOAD_TIME_KEY, task.uploadTime().orNull());
        TranslatorUtils.from(dbo, REMOTE_ID_KEY, task.remoteId().orNull());
        TranslatorUtils.fromIterable(dbo, REMOTE_STATUSES_KEY, task.remoteResponses(), ResponseTranslator.toDBObject());
        TranslatorUtils.from(dbo, STATUS_KEY, task.status().name());
        
        return dbo;
    }

    public static Task fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        
        return Task.builder()
                .withId(TranslatorUtils.toLong(dbo, MongoConstants.ID))
                .withPublisher(Publisher.fromKey(TranslatorUtils.toString(dbo, PUBLISHER_KEY)).requireValue())
                .withAction(Action.valueOf(TranslatorUtils.toString(dbo, ACTION_KEY)))
                .withUploadTime(TranslatorUtils.toDateTime(dbo, UPLOAD_TIME_KEY))
                .withRemoteId(TranslatorUtils.toString(dbo, REMOTE_ID_KEY))
                .withContent(TranslatorUtils.toString(dbo, CONTENT_KEY))
                .withRemoteResponses(TranslatorUtils.toIterable(dbo, REMOTE_STATUSES_KEY, ResponseTranslator.fromDBObject()).or(ImmutableList.<Response>of()))
                .withStatus(Status.valueOf(TranslatorUtils.toString(dbo, STATUS_KEY)))
                .build();
    }
    
    public static Function<DBObject, Task> fromDBObjects() {
        return new Function<DBObject, Task>() {
            @Override
            public Task apply(DBObject input) {
                return fromDBObject(input);
            }
        };
    }
}
