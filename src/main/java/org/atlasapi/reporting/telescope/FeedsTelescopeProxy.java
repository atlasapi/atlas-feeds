package org.atlasapi.reporting.telescope;

import java.time.LocalDateTime;

import telescope_client_shaded.com.metabroadcast.columbus.telescope.api.EntityState;
import telescope_client_shaded.com.metabroadcast.columbus.telescope.api.Event;
import telescope_client_shaded.com.metabroadcast.columbus.telescope.api.Process;
import com.metabroadcast.common.media.MimeType;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedsTelescopeProxy extends TelescopeProxy {

    private static final Logger log = LoggerFactory.getLogger(FeedsTelescopeProxy.class);

    protected FeedsTelescopeProxy(Process process) {
        super(process);
    }

    /**
     * If there are initialization errors the telescope you will get might be unable to report, but
     * it will fail graciously so it is safe to use.
     * <p>
     * Use by .startReporting() first, then report any events, then finally .endReporting()
     */
    public static FeedsTelescopeProxy create(TelescopeReporter reporterName) {
        Process process = TelescopeUtilityMethods.getProcess(reporterName);
        FeedsTelescopeProxy telescopeProxy = new FeedsTelescopeProxy(process);

        return telescopeProxy;
    }

    public void reportSuccessfulEvent(String atlasItemId, String payload) {
        if (!isStarted()) {
            log.error("It was attempted to report atlasItem={}, but the telescope client was not started.", atlasItemId );
            return;
        }
        if (isFinished()) { //we can still report, but it shouldn't happen
            log.warn("atlasItem={} was reported to telescope client={} after it has finished reporting.", atlasItemId, getTaskId() );
        }
        try { //fail graciously by reporting nothing, but print a full stack so we know who caused this
            if (atlasItemId == null) {
                throw new IllegalArgumentException("No atlasId was given");
            }
        }
        catch(IllegalArgumentException e){
            log.error( "Cannot report an atlas event without an atlasId", e);
            return;
        }

        //if all went well
        Event reportEvent = Event.builder()
                .withStatus(Event.Status.SUCCESS)
                .withType(Event.Type.INGEST)
                .withEntityState(EntityState.builder()
                        .withAtlasId(atlasItemId)
                        .withRaw(payload)
                        .withRawMime(MimeType.APPLICATION_JSON.toString())
                        .build()
                )
                .withTaskId(getTaskId())
                .withTimestamp(LocalDateTime.now())
                .build();
       telescopeClient.createEvents(ImmutableList.of(reportEvent));
    }

    //convenience method for the most common reporting Format
    public void reportSuccessfulEvent(Long dbId, String payload) {
        reportSuccessfulEvent(encode(dbId), payload);
    }

    public void reportFailedEventWithWarning(String atlasItemId, String warningMsg, String payload) {
        if (!isStarted()) {
            log.error("It was attempted to report atlasId={}, but the telescope client was not started.", atlasItemId);
            return;
        }
        if (isFinished()) { //we can still report, but it shouldn't happen
            log.warn( "atlasId={} was reported to telescope client={} after it had finished reporting.", atlasItemId, getTaskId());
        }
        try { //fail graciously by reporting nothing, but print a full stack so we know who caused this
            if (atlasItemId == null) {
                throw new IllegalArgumentException("No atlasId was given");
            }
        }
        catch(IllegalArgumentException e){
            log.error( "Cannot report an atlas event without an atlasId. This report already had a warning message={}",warningMsg, e);
            return;
        }

        Event reportEvent = Event.builder()
                .withStatus(Event.Status.FAILURE)
                .withType(Event.Type.INGEST)
                .withEntityState(
                        EntityState.builder()
                        .withAtlasId(atlasItemId)
                        .withWarning(warningMsg)
                        .withRaw(payload)
                        .withRawMime(MimeType.APPLICATION_JSON.toString())
                        .build()
                )
                .withTaskId(getTaskId())
                .withTimestamp(LocalDateTime.now())
                .build();
        telescopeClient.createEvents(ImmutableList.of(reportEvent));
    }

    //convenience method for the most common reporting Format
    public void reportFailedEventWithWarning(long dbId, String warningMsg, String payload) {
        reportFailedEventWithWarning(encode(dbId), warningMsg, payload);
    }

    public void reportFailedEventWithError(String errorMsg, String payload) {
        if (!isStarted()) {
            log.error( "It was attempted to report an error to telescope, but the client was not started." );
            return;
        }
        if (isFinished()) { //we can still report, but it shouldn't happen
            log.warn("An error was reported to telescope after the telescope client={} has finished reporting.", getTaskId() );
        }

        Event reportEvent = Event.builder()
                .withStatus(Event.Status.FAILURE)
                .withType(Event.Type.INGEST)
                .withEntityState(EntityState.builder()
                        .withError(errorMsg)
                        .withRaw(payload)
                        .withRawMime(MimeType.APPLICATION_JSON.toString())
                        .build()
                )
                .withTaskId(getTaskId())
                .withTimestamp(LocalDateTime.now())
                .build();
        telescopeClient.createEvents(ImmutableList.of(reportEvent));
    }
}
