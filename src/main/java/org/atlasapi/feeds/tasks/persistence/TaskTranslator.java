package org.atlasapi.feeds.tasks.persistence;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TaskTranslator {

    private static final Logger log = LoggerFactory.getLogger(TaskTranslator.class);

    static final String ATLAS_DB_ID = "atlasDbId";
    static final String PUBLISHER_KEY = "publisher";
    static final String CREATED_KEY = "created";
    static final String ACTION_KEY = "action";
    static final String CONTENT_KEY = "content";
    static final String UPLOAD_TIME_KEY = "uploadTime";
    static final String REMOTE_ID_KEY = "remoteId";
    static final String PAYLOAD_KEY = "payload";
    static final String DESTINATION_TYPE_KEY = "destinationType";
    static final String REMOTE_STATUSES_KEY = "remoteStatuses";
    static final String STATUS_KEY = "status";
    static final String LAST_ERROR_KEY = "lastError";
    static final String ELEMENT_TYPE_KEY = "elementType";
    static final String ELEMENT_ID_KEY = "elementId";
    static final String MANUALLY_CREATED = "manuallyCreated";

    public static DBObject toDBObject(Task task) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, task.id());
        TranslatorUtils.from(dbo, ATLAS_DB_ID, task.atlasDbId());
        TranslatorUtils.fromDateTime(dbo, CREATED_KEY, task.created());
        TranslatorUtils.from(dbo, PUBLISHER_KEY, task.publisher().key());
        TranslatorUtils.from(dbo, ACTION_KEY, task.action().name());
        TranslatorUtils.from(dbo, STATUS_KEY, task.status().name());
        writeDestination(task.destination(), dbo);
        
        TranslatorUtils.fromDateTime(dbo, UPLOAD_TIME_KEY, task.uploadTime().orNull());
        TranslatorUtils.from(dbo, REMOTE_ID_KEY, task.remoteId().orNull());
        TranslatorUtils.from(dbo, PAYLOAD_KEY, (DBObject) PayloadTranslator.toDBObject(task.payload().orNull()));
        TranslatorUtils.from(dbo, LAST_ERROR_KEY, task.lastError().orNull());
        
        TranslatorUtils.fromIterable(dbo, REMOTE_STATUSES_KEY, task.remoteResponses(), ResponseTranslator.toDBObject());
        TranslatorUtils.from(dbo, MANUALLY_CREATED, task.isManuallyCreated());

        return dbo;
    }

    private static void writeDestination(Destination destination, DBObject dbo) {
        TranslatorUtils.from(dbo, DESTINATION_TYPE_KEY, destination.type().name());
        
        switch (destination.type()) {
        case RADIOPLAYER:
            break;
        case YOUVIEW:
            YouViewDestination yvDest = (YouViewDestination) destination;
            TranslatorUtils.from(dbo, CONTENT_KEY, yvDest.contentUri());
            TranslatorUtils.from(dbo, ELEMENT_TYPE_KEY, yvDest.elementType().name());
            TranslatorUtils.from(dbo, ELEMENT_ID_KEY, yvDest.elementId());    
            break;
        default:
            log.error("attempted to write unrecognised task destination type {}", destination.type().name());
            break;
        }
    }

    public static Task fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        
        return Task.builder()
                .withId(TranslatorUtils.toLong(dbo, MongoConstants.ID))
                .withAtlasDbId(TranslatorUtils.toLong(dbo, ATLAS_DB_ID))
                .withCreated(TranslatorUtils.toDateTime(dbo, CREATED_KEY))
                .withPublisher(Publisher.fromKey(TranslatorUtils.toString(dbo, PUBLISHER_KEY)).requireValue())
                .withAction(Action.valueOf(TranslatorUtils.toString(dbo, ACTION_KEY)))
                .withDestination(readDestination(dbo))
                .withStatus(Status.valueOf(TranslatorUtils.toString(dbo, STATUS_KEY)))
                .withUploadTime(TranslatorUtils.toDateTime(dbo, UPLOAD_TIME_KEY))
                .withRemoteId(TranslatorUtils.toString(dbo, REMOTE_ID_KEY))
                .withPayload(PayloadTranslator.fromDBObject(TranslatorUtils.toDBObject(dbo, PAYLOAD_KEY)))
                .withRemoteResponses(TranslatorUtils.toIterable(dbo, REMOTE_STATUSES_KEY, ResponseTranslator.fromDBObject()).or(ImmutableList.<Response>of()))
                .withLastError(TranslatorUtils.toString(dbo, LAST_ERROR_KEY))
                .withManuallyCreated(TranslatorUtils.toBoolean(dbo, MANUALLY_CREATED))
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

    private static Destination readDestination(DBObject dbo) {
        DestinationType type = DestinationType.valueOf(
                // This is necessary to deal with those tasks predating the destination type.
                // These will naturally roll off the end of the task window and so once that has
                // happened the Objects.firstNonNull call can be removed
                Objects.firstNonNull(TranslatorUtils.toString(dbo, DESTINATION_TYPE_KEY), "YOUVIEW")
        );

        switch (type) {
        case RADIOPLAYER:
            throw new UnsupportedOperationException("RadioPlayer is not yet supported by the Tasks system");
        case YOUVIEW:
            return new YouViewDestination(
                    TranslatorUtils.toString(dbo, CONTENT_KEY), 
                    TVAElementType.valueOf(TranslatorUtils.toString(dbo, ELEMENT_TYPE_KEY)), 
                    TranslatorUtils.toString(dbo, ELEMENT_ID_KEY)
            );
        default:
            throw new RuntimeException("encountered unknown task destination type " + type.name());
        }
    }
}
