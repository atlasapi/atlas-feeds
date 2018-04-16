package org.atlasapi.reporting.telescope;

import java.util.Optional;

import javax.annotation.Nullable;

import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadResult;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;

import com.metabroadcast.columbus.telescope.api.EntityState;
import com.metabroadcast.columbus.telescope.api.Environment;
import com.metabroadcast.columbus.telescope.api.Event;
import com.metabroadcast.columbus.telescope.client.EntityType;
import com.metabroadcast.columbus.telescope.client.TelescopeClientImpl;
import com.metabroadcast.columbus.telescope.client.TelescopeReporter;
import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.media.MimeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;

public class FeedsTelescopeReporter extends TelescopeReporter {

    private @Autowired ChannelResolver channelResolver;
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


    public void reportEvent(RadioPlayerUploadResult result) {

        if (SUCCESS.equals(result.getUpload().type())) {
            EntityState.Builder entityState = entityStateFromRadioPlayerResult(result);
            if(entityState != null){
                reportSuccessfulEventGeneric(entityState, null);
            } else {
                reportFailedEvent("The event was reported as successful, but a proper telescope "
                                  + "report could not be constructed by the given UploadResult.",
                        EntityType.CHANNEL.getVerbose(), result.toString());
            }
        } else {
            reportFailedEvent(result.getUpload().type().toNiceString(),
                    EntityType.CHANNEL.getVerbose(), result.toString());
        }
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

    public void reportFailedEventFromTask(Task task, String errorMsg) {
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

        String entityType = null;
        try { //Because I got trust issues
            //see if you can get a type from the destination
            if (task.destination() != null
                //only YOUVIEW destinations got types.
                && task.destination().type() == Destination.DestinationType.YOUVIEW) {

                YouViewDestination yvDest = (YouViewDestination) task.destination();
                entityType = EntityType.getVerbose(yvDest.elementType().name());
                //if it is not in the enumeration, use whatever you can
                if (entityType == null) {
                    entityType = yvDest.elementType().name();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract an entityState from a task. Essentially I caught that "
                     + "exception to prevent failing, and felt bad to just swallow it.", e);
        }

        return entityStateFromStrings(atlasId, entityType, payload);
    }

    @Nullable
    private EntityState.Builder entityStateFromRadioPlayerResult(RadioPlayerUploadResult result) {
        if (result == null) {
          return null;
        }

        try {
            Optional<Channel> channelOptional =
                    channelResolver.fromUri(result.getService().getServiceUri()).toOptional();

            String atlasId = Long.toString(channelOptional.get().getId());
            return entityStateFromStrings(atlasId, EntityType.CHANNEL.getVerbose(), result.getPayload());
        } catch (Exception e) {
            log.warn("Failed to extract an entityState from a RadioPlayer upload result. "
                     + "It won't be reported to telescope.", e);
        }

        return null;
    }

    private EntityState.Builder entityStateFromStrings(String atlasId, String entityType,
            String payload) {
        return EntityState.builder()
                .withAtlasId(atlasId)
                .withType(entityType)
                .withRaw(payload)
                .withRawMime(MimeType.APPLICATION_XML.toString());
    }
}
