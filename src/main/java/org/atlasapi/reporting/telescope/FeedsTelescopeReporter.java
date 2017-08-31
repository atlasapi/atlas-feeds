package org.atlasapi.reporting.telescope;

import java.time.LocalDateTime;

import com.metabroadcast.columbus.telescope.api.EntityState;
import com.metabroadcast.columbus.telescope.api.Event;
import com.metabroadcast.columbus.telescope.client.TelescopeReporter;
import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.properties.Configurer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedsTelescopeReporter extends TelescopeReporter {

    private static final Logger log = LoggerFactory.getLogger(FeedsTelescopeReporter.class);

    protected FeedsTelescopeReporter(TelescopeReporterName reporterName) {
        //Surprisingly, this will draw the actual configuration from atlas.
        super(reporterName, Configurer.get("telescope.environment").get(), Configurer.get("telescope.host").get());
    }

    /**
     * If there are initialization errors the telescope you will get might be unable to report, but
     * it will fail graciously so it is safe to use.
     * <p>
     * Use by .startReporting() first, then report any events, then finally .endReporting()
     */
    public static FeedsTelescopeReporter create(TelescopeReporterName reporterName) {
        return new FeedsTelescopeReporter(reporterName);
    }

    private void reportSuccessfulEventGeneric(
            String atlasItemId,
            String warningMsg,
            String payload
    ) {
        try { //fail graciously by reporting nothing, but print a full stack so we know who caused this
            if (atlasItemId == null) {
                throw new IllegalArgumentException("No atlasId was given");
            }
        }
        catch(IllegalArgumentException e){
            log.error( "Cannot report a successful event to telescope, without an atlasId", e);
            return;
        }

        EntityState.Builder entityState = EntityState.builder()
                .withAtlasId(atlasItemId)
                .withRaw(payload)
                .withRawMime(MimeType.APPLICATION_JSON.toString());

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

    public void reportSuccessfulEvent(String atlasItemId, String payload) {
       reportSuccessfulEventGeneric(atlasItemId, null, payload);
    }

    //convenience method for the most common reporting Format
    public void reportSuccessfulEvent(Long dbId, String payload) {
        reportSuccessfulEvent(encode(dbId), payload);
    }

    public void reportSuccessfulEventWithWarning(String atlasItemId, String warningMsg, String payload) {
        reportSuccessfulEventGeneric(atlasItemId, warningMsg, payload);
    }

    //convenience method for the most common reporting Format
    public void reportSuccessfulEventWithWarning(long dbId, String warningMsg, String payload) {
        reportSuccessfulEventWithWarning(encode(dbId), warningMsg, payload);
    }

    /**
     * Convenience method for {@link #reportFailedEventWithError(String, String, MimeType)}
     * @param errorMsg
     * @param payload Default payload is XML
     */
    public void reportFailedEventWithError(String errorMsg, String payload) {
        reportFailedEventWithError(errorMsg,payload,MimeType.APPLICATION_XML);
    }

    public void reportFailedEventWithError(String errorMsg, String payload, MimeType mimeType) {
        Event event = Event.builder()
                .withStatus(Event.Status.FAILURE)
                .withType(Event.Type.UPLOAD)
                .withEntityState(EntityState.builder()
                        .withError(errorMsg)
                        .withRaw(payload)
                        .withRawMime(mimeType.toString())
                        .build()
                )
                .withTaskId(getTaskId())
                .withTimestamp(LocalDateTime.now())
                .build();
        reportEvent(event);
    }
}
