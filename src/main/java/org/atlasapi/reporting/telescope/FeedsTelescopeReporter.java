package org.atlasapi.reporting.telescope;

import java.time.LocalDateTime;

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

    public void reportSuccessfulEvent(Long dbId, String payload) {
        reportSuccessfulEventGeneric(encode(dbId), null, payload);
    }

    public void reportSuccessfulEventWithWarning(long dbId, String warningMsg, String payload) {
        reportSuccessfulEventGeneric(encode(dbId), warningMsg, payload);
    }

    private void reportSuccessfulEventGeneric(
            String atlasItemId,
            String warningMsg,
            String payload
    ) {
        //fail graciously by reporting nothing, but print a full stack so we know who caused this
        if (atlasItemId == null) {
            log.error(
                    "Cannot report a successful event to telescope, without an atlasId",
                    new IllegalArgumentException("No atlasId was given"));
            return;
        }

        EntityState.Builder entityState = EntityState.builder()
                .withAtlasId(atlasItemId)
                .withRaw(payload)
                .withRawMime(MimeType.APPLICATION_XML.toString());

        if (warningMsg != null) {
            entityState.withWarning(warningMsg);
        }

        Event event = super.getEventBuilder()
                .withType(Event.Type.UPLOAD)
                .withStatus(Event.Status.SUCCESS)
                .withEntityState(entityState.build())
                .build();

        reportEvent(event);
    }

    public void reportFailedEvent(String errorMsg) {
        //cant have null, telescope requires either an atlasId, or error+raw.
        reportFailedEventWithAtlasId("", errorMsg, "");
    }

    public void reportFailedEvent(String errorMsg, String payload) {
        reportFailedEventWithAtlasId("", errorMsg, payload);
    }

    public void reportFailedEventWithAtlasId(long dbId, String errorMsg, String payload) {
        reportFailedEventWithAtlasId(encode(dbId), errorMsg, payload);
    }

    public void reportFailedEventWithAtlasId(long dbId, String errorMsg) {
        reportFailedEventWithAtlasId(encode(dbId), errorMsg, "");
    }

    public void reportFailedEventWithAtlasId(Task task, String errorMsg, String payload) {
        if (task != null && task.atlasDbId() != null) {
            reportFailedEventWithAtlasId(encode(task.atlasDbId()), errorMsg, payload);
        }else{
            reportFailedEvent(errorMsg, "");
        }
    }

    public void reportFailedEventWithAtlasId(Task task, String errorMsg) {
        reportFailedEventWithAtlasId(task, errorMsg, "");
    }

    /**
     * At least (atlasId || ( errorMsg && payload )) must be defined, otherwise telescope with throw exceptions.
     */
    private void reportFailedEventWithAtlasId(String atlasId, String errorMsg, String payload) {
        Event event = Event.builder()
                .withStatus(Event.Status.FAILURE)
                .withType(Event.Type.UPLOAD)
                .withEntityState(EntityState.builder()
                        .withAtlasId(atlasId)
                        .withError(errorMsg)
                        .withRaw(payload)
                        .withRawMime(MimeType.APPLICATION_XML.toString())
                        .build()
                )
                .withTaskId(getTaskId())
                .withTimestamp(LocalDateTime.now())
                .build();

        reportEvent(event);
    }
}
