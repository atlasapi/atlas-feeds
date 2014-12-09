package org.atlasapi.feeds.youview.tasks.persistence;

import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class TaskTranslator {
    
    static final String PUBLISHER_KEY = "publisher";
    static final String ACTION_KEY = "action";
    static final String CONTENT_KEY = "content";
    static final String UPLOAD_TIME_KEY = "uploadTime";
    static final String REMOTE_ID_KEY = "remoteId";
    static final String PAYLOAD_KEY = "payload";
    static final String REMOTE_STATUSES_KEY = "remoteStatuses";
    static final String STATUS_KEY = "status";
    static final String ELEMENT_TYPE_KEY = "elementType";
    static final String ELEMENT_ID_KEY = "elementId";
    
    private static final String NO_DATA = "No Data available";
    
    public static DBObject toDBObject(Task task) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, task.id());
        TranslatorUtils.from(dbo, PUBLISHER_KEY, task.publisher().key());
        TranslatorUtils.from(dbo, ACTION_KEY, task.action().name());
        TranslatorUtils.from(dbo, CONTENT_KEY, task.content());
        TranslatorUtils.fromDateTime(dbo, UPLOAD_TIME_KEY, task.uploadTime().orNull());
        TranslatorUtils.from(dbo, REMOTE_ID_KEY, task.remoteId().orNull());
        TranslatorUtils.from(dbo, PAYLOAD_KEY, PayloadTranslator.toDBObject(task.payload()));
        TranslatorUtils.from(dbo, ELEMENT_TYPE_KEY, task.elementType().name());
        TranslatorUtils.from(dbo, ELEMENT_ID_KEY, task.elementId());
        TranslatorUtils.fromIterable(dbo, REMOTE_STATUSES_KEY, task.remoteResponses(), ResponseTranslator.toDBObject());
        TranslatorUtils.from(dbo, STATUS_KEY, task.status().name());
        
        return dbo;
    }

    public static Task fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        
        DateTime uploadTime = TranslatorUtils.toDateTime(dbo, UPLOAD_TIME_KEY);
        Payload payload = PayloadTranslator.fromDBObject(TranslatorUtils.toDBObject(dbo, PAYLOAD_KEY));
        if (payload == null) {
            payload = createDefaultPayload(uploadTime);
        }
        
        return Task.builder()
                .withId(TranslatorUtils.toLong(dbo, MongoConstants.ID))
                .withPublisher(Publisher.fromKey(TranslatorUtils.toString(dbo, PUBLISHER_KEY)).requireValue())
                .withAction(Action.valueOf(TranslatorUtils.toString(dbo, ACTION_KEY)))
                .withUploadTime(uploadTime)
                .withRemoteId(TranslatorUtils.toString(dbo, REMOTE_ID_KEY))
                .withPayload(payload)
                .withContent(TranslatorUtils.toString(dbo, CONTENT_KEY))
                .withElementType(TVAElementType.valueOf(TranslatorUtils.toString(dbo, ELEMENT_TYPE_KEY)))
                .withElementId(TranslatorUtils.toString(dbo, ELEMENT_ID_KEY))
                .withRemoteResponses(TranslatorUtils.toIterable(dbo, REMOTE_STATUSES_KEY, ResponseTranslator.fromDBObject()).or(ImmutableList.<Response>of()))
                .withStatus(Status.valueOf(TranslatorUtils.toString(dbo, STATUS_KEY)))
                .build();
    }

    private static Payload createDefaultPayload(DateTime uploadTime) {
        if (uploadTime == null) {
            return new Payload(NO_DATA, new DateTime(0));
        }
        return new Payload(NO_DATA, uploadTime);
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
