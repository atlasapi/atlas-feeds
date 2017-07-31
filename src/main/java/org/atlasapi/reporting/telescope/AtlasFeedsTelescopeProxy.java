package org.atlasapi.reporting.telescope;

import java.time.LocalDateTime;

import com.metabroadcast.columbus.telescope.api.EntityState;
import com.metabroadcast.columbus.telescope.api.Event;
import com.metabroadcast.columbus.telescope.api.Process;
import com.metabroadcast.common.media.MimeType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtlasFeedsTelescopeProxy extends TelescopeProxy {

    private static final Logger log = LoggerFactory.getLogger(AtlasFeedsTelescopeProxy.class);

    protected AtlasFeedsTelescopeProxy(Process process) {
        super(process);
    }

    /**
     * This factory will always give you a telescope (never null). If there are initialization
     * errors the telescope you will get might be unable to report, and fail graciously.
     */
    public static AtlasFeedsTelescopeProxy create(TelescopeReporter reporterName) {
        Process process = TelescopeUtilityMethods.getProcess(reporterName);
        AtlasFeedsTelescopeProxy telescopeProxy = new AtlasFeedsTelescopeProxy(process);

        return telescopeProxy;
    }

    public void reportSuccessfulEvent(String atlasItemId, String payload) {
        if (!allowedToReport()) {
            return;
        }
        Event reportEvent = Event.builder()
                .withStatus(Event.Status.SUCCESS)
                .withType(Event.Type.INGEST)
                .withEntityState(EntityState.builder()
                        .withAtlasId(atlasItemId)
                        .withRaw(payload)
                        .withRawMime(MimeType.APPLICATION_JSON.toString())
                        .build()
                )
                .withTaskId(taskId)
                .withTimestamp(LocalDateTime.now())
                .build();

        telescopeClient.createEvents(ImmutableList.of(reportEvent));

        log.debug(
                "Reported successfully event with taskId={}, eventId={}",
                taskId,
                reportEvent.getId().orElse("null")
        );

    }

    //convenience method for the most common reporting Format
    public void reportSuccessfulEvent(long dbId, String payload) {
        reportSuccessfulEvent(encode(dbId), payload);
    }

    public void reportFailedEventWithWarning(String atlasItemId, String warningMsg,
            Object objectToSerialise) {
        if (!allowedToReport()) {
            return;
        }
        try {
            Event reportEvent = Event.builder()
                    .withStatus(Event.Status.FAILURE)
                    .withType(Event.Type.INGEST)
                    .withEntityState(EntityState.builder()
                            .withAtlasId(atlasItemId)
                            .withWarning(warningMsg)
                            .withRaw(objectMapper.writeValueAsString(objectToSerialise))
                            .withRawMime(MimeType.APPLICATION_JSON.toString())
                            .build()
                    )
                    .withTaskId(taskId)
                    .withTimestamp(LocalDateTime.now())
                    .build();
            telescopeClient.createEvents(ImmutableList.of(reportEvent));

            log.debug(
                    "Reported successfully a FAILED event, taskId={}, warning={}",
                    taskId,
                    warningMsg
            );
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert the given object to a JSON string.", e);
        }
    }

    //convenience method for the most common reporting Format
    public void reportFailedEventWithWarning(
            long dbId, String warningMsg, Object objectToSerialise) {
        reportFailedEventWithWarning(encode(dbId), warningMsg, objectToSerialise);
    }

    public void reportFailedEventWithError(String errorMsg, Object objectToSerialise) {
        if (!allowedToReport()) {
            return;
        }
        try {
            Event reportEvent = Event.builder()
                    .withStatus(Event.Status.FAILURE)
                    .withType(Event.Type.INGEST)
                    .withEntityState(EntityState.builder()
                            .withError(errorMsg)
                            .withRaw(objectMapper.writeValueAsString(objectToSerialise))
                            .withRawMime(MimeType.APPLICATION_JSON.toString())
                            .build()
                    )
                    .withTaskId(taskId)
                    .withTimestamp(LocalDateTime.now())
                    .build();
            telescopeClient.createEvents(ImmutableList.of(reportEvent));

            log.debug(
                    "Reported successfully a FAILED event with taskId={}, error={}",
                    taskId,
                    errorMsg
            );
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert the given object to a JSON string.", e);
        }
    }
}
