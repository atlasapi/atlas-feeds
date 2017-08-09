package org.atlasapi.reporting.telescope;

import java.time.LocalDateTime;

import com.metabroadcast.columbus.telescope.api.EntityState;
import com.metabroadcast.columbus.telescope.api.Event;
import com.metabroadcast.columbus.telescope.client.TelescopeReporter;
import com.metabroadcast.columbus.telescope.client.TelescopeReporterNames;
import com.metabroadcast.common.media.MimeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedsTelescopeReporter extends TelescopeReporter {

    private static final Logger log = LoggerFactory.getLogger(FeedsTelescopeReporter.class);

    protected FeedsTelescopeReporter(TelescopeReporterNames reporterName) {
        super(reporterName, TelescopeConfiguration.ENVIRONMENT, TelescopeConfiguration.TELESCOPE_HOST);
    }

    /**
     * If there are initialization errors the telescope you will get might be unable to report, but
     * it will fail graciously so it is safe to use.
     * <p>
     * Use by .startReporting() first, then report any events, then finally .endReporting()
     */
    public static FeedsTelescopeReporter create(TelescopeReporterNames reporterName) {

        FeedsTelescopeReporter telescopeProxy = new FeedsTelescopeReporter(reporterName);

        return telescopeProxy;
    }

    public void reportSuccessfulEvent(String atlasItemId, String payload) {
        try { //fail graciously by reporting nothing, but print a full stack so we know who caused this
            if (atlasItemId == null) {
                throw new IllegalArgumentException("No atlasId was given");
            }
        }
        catch(IllegalArgumentException e){
            log.error( "Cannot report an atlas event without an atlasId", e);
            return;
        }

        if (!isStarted()) {
            log.error("It was attempted to report atlasItem={}, but the telescope client was not started.", atlasItemId );
            return;
        }
        if (isFinished()) { //we can still report, but it shouldn't happen
            log.warn("atlasItem={} was reported to telescope client={} after it has finished reporting.", atlasItemId, getTaskId() );
        }

        //if all went well
        Event event = Event.builder()
                .withStatus(Event.Status.SUCCESS)
                .withType(Event.Type.UPLOAD)
                .withEntityState(EntityState.builder()
                        .withAtlasId(atlasItemId)
                        .withRaw(payload)
                        .withRawMime(MimeType.APPLICATION_XML.toString())
                        .build()
                )
                .withTaskId(getTaskId())
                .withTimestamp(LocalDateTime.now())
                .build();
       reportEvent(event);
    }

    //convenience method for the most common reporting Format
    public void reportSuccessfulEvent(Long dbId, String payload) {
        reportSuccessfulEvent(encode(dbId), payload);
    }

    public void reportFailedEventWithWarning(String atlasItemId, String warningMsg, String payload) {
        try { //fail graciously by reporting nothing, but print a full stack so we know who caused this
            if (atlasItemId == null) {
                throw new IllegalArgumentException("No atlasId was given");
            }
        }
        catch(IllegalArgumentException e){
            log.error( "Cannot report an atlas event without an atlasId. This report already had a warning message={}",warningMsg, e);
            return;
        }

        if (!isStarted()) {
            log.error("It was attempted to report atlasId={}, but the telescope client was not started.", atlasItemId);
            return;
        }
        if (isFinished()) { //we can still report, but it shouldn't happen
            log.warn( "atlasId={} was reported to telescope client={} after it had finished reporting.", atlasItemId, getTaskId());
        }

        Event event = Event.builder()
                .withStatus(Event.Status.FAILURE)
                .withType(Event.Type.UPLOAD)
                .withEntityState(
                        EntityState.builder()
                        .withAtlasId(atlasItemId)
                        .withWarning(warningMsg)
                        .withRaw(payload)
                        .withRawMime(MimeType.APPLICATION_XML.toString())
                        .build()
                )
                .withTaskId(getTaskId())
                .withTimestamp(LocalDateTime.now())
                .build();
        reportEvent(event);
    }

    //convenience method for the most common reporting Format
    public void reportFailedEventWithWarning(long dbId, String warningMsg, String payload) {
        reportFailedEventWithWarning(encode(dbId), warningMsg, payload);
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
        if (!isStarted()) {
            log.error( "It was attempted to report an error to telescope, but the client was not started." );
            return;
        }
        if (isFinished()) { //we can still report, but it shouldn't happen
            log.warn("An error was reported to telescope after the telescope client={} has finished reporting.", getTaskId() );
        }

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