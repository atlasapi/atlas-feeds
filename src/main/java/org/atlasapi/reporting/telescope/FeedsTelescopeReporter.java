package org.atlasapi.reporting.telescope;

import org.atlasapi.feeds.tasks.Task;

import com.metabroadcast.columbus.telescope.api.EntityState;
import com.metabroadcast.columbus.telescope.api.Environment;
import com.metabroadcast.columbus.telescope.api.Event;
import com.metabroadcast.columbus.telescope.client.TelescopeClientImpl;
import com.metabroadcast.columbus.telescope.client.TelescopeReporter;
import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;
import com.metabroadcast.common.media.MimeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedsTelescopeReporter extends TelescopeReporter {

    private static final Logger log = LoggerFactory.getLogger(FeedsTelescopeReporter.class);

    /**
     * If there are initialization errors the telescope you will get might be unable to report, but
     * it will fail graciously so it is safe to use.
     * <p>
     * Use by .startReporting() first, then report any events, then finally .endReporting()
     */
    protected FeedsTelescopeReporter(
            TelescopeReporterName reporterName,
            Environment environment,
            TelescopeClientImpl client) {
        super(reporterName, environment, client);
    }

    public void reportSuccessfulEvent(Task task){
        reportSuccessfulEventWithWarning(task, null);
    }

    public void reportSuccessfulEventWithWarning(
            long dbId,
            String warningMsg,
            String entityType,
            String payload) {

        EntityState.Builder entityState = entityStateFromStrings(encode(dbId), entityType, payload);
        reportSuccessfulEventGeneric(entityState, warningMsg);
    }

    public void reportSuccessfulEventWithWarning(Task task, String warningMsg){
        EntityState.Builder entityState = entityStateFromTask(task);
        reportSuccessfulEventGeneric(entityState, warningMsg);
    }

    private void reportSuccessfulEventGeneric(
            String atlasItemId,
            String warningMsg,
            String entityType,
            String payload) {

        EntityState.Builder entityState = entityStateFromStrings(atlasItemId, entityType, payload);
        reportFailedEventGeneric(entityState, warningMsg);
    }

    private void reportSuccessfulEventGeneric(
            EntityState.Builder entityState,
            String warningMsg) {

        if (warningMsg != null) {
            entityState.withWarning(warningMsg);
        }

        EntityState entityStateBuilt = entityState.build();
        //fail graciously by reporting nothing, but print a full stack so we know who caused this
        if (!entityStateBuilt.getAtlasId().isPresent()) {
            log.error(
                    "Cannot report a successful event to telescope, without an atlasId",
                    new NullPointerException("No atlasId was given"));
            return;
        }

        Event event = getEventBuilder()
                .withType(Event.Type.UPLOAD)
                .withStatus(Event.Status.SUCCESS)
                .withEntityState(entityStateBuilt)
                .build();

        reportEvent(event);
    }

    public void reportFailedEvent(String errorMsg) {
        reportFailedEvent( errorMsg, null);
    }

    public void reportFailedEvent(String errorMsg, String entityType) {
        //cant have null, telescope requires either an atlasId, or error+raw.
        reportFailedEvent( errorMsg, entityType, "");
    }

    public void reportFailedEvent(String errorMsg, String entityType, String payload) {
        reportFailedEventGeneric("", errorMsg, entityType, payload);
    }

    public void reportFailedEvent(Task task, String errorMsg) {
       reportFailedEventGeneric( entityStateFromTask(task), errorMsg);
    }

    private void reportFailedEventGeneric(
            String atlasId,
            String errorMsg,
            String entityType,
            String payload) {
        EntityState.Builder entityState = entityStateFromStrings(atlasId, entityType, payload);
        reportFailedEventGeneric(entityState, errorMsg);
    }

    /**
     * At least (atlasId || ( errorMsg && payload )) must be defined, otherwise telescope with throw
     * exceptions. reportEvent() will handle the errors though.
     */
    private void reportFailedEventGeneric(EntityState.Builder entityState, String errorMsg){
        entityState.withError(errorMsg);
        Event event = getEventBuilder()
                .withType(Event.Type.UPLOAD)
                .withStatus(Event.Status.FAILURE)
                .withEntityState(entityState.build())
                .build();

        reportEvent(event);
    }

    private EntityState.Builder entityStateFromTask(Task task) {
        if (task == null) {
            //this will eventually fail (silently). Look at reportFailedEventGeneric comments.
            //However it should not happen either, and its here to prevent NullPointerExceptions.
            return EntityState.builder();
        }
        String atlasId = task.atlasDbId() != null
                         ? encode(task.atlasDbId())
                        : null;
        String payload = task.payload().isPresent()
                         ? task.payload().get().payload()
                         : null;
        String entityType = task.entityType().isPresent()
                            ? task.entityType().get()
                            : null;

        return entityStateFromStrings(atlasId, entityType, payload);
    }

    private EntityState.Builder entityStateFromStrings(String atlasId, String entityType, String payload){
        return EntityState.builder()
                .withAtlasId(atlasId)
                .withType(entityType)
                .withRaw(payload)
                .withRawMime(MimeType.APPLICATION_XML.toString());
    }
}
