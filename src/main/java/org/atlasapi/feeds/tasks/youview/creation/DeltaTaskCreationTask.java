package org.atlasapi.feeds.tasks.youview.creation;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelQuery;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.channel.ChannelType;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;


public class DeltaTaskCreationTask extends TaskCreationTask {

    private static final Logger log = LoggerFactory.getLogger(DeltaTaskCreationTask.class);
    private static final Ordering<Content> HIERARCHICAL_ORDER = new HierarchicalOrdering();

    private final YouViewContentResolver contentResolver;
    private final ChannelResolver channelResolver;
    private final UpdateTask updateTask;

    public DeltaTaskCreationTask(
            YouViewLastUpdatedStore lastUpdatedStore,
            Publisher publisher,
            ContentHierarchyExpander hierarchyExpander,
            IdGenerator idGenerator,
            TaskStore taskStore,
            TaskCreator taskCreator,
            PayloadCreator payloadCreator,
            UpdateTask updateTask,
            YouViewContentResolver contentResolver,
            YouViewPayloadHashStore payloadHashStore,
            ChannelResolver channelResolver
    ) {
        super(
                lastUpdatedStore,
                publisher,
                hierarchyExpander,
                idGenerator,
                taskStore,
                taskCreator,
                payloadCreator,
                payloadHashStore
        );
        this.channelResolver = checkNotNull(channelResolver);
        this.contentResolver = checkNotNull(contentResolver);
        this.updateTask = checkNotNull(updateTask);
    }

    @Override
    protected void runTask() {
        Optional<DateTime> lastUpdated = getLastUpdatedTime();
        if (!lastUpdated.isPresent()) {
            throw new RuntimeException("The bootstrap has not successfully run. Please run the bootstrap upload and ensure that it succeeds before running the delta upload.");
        }
        
        Optional<DateTime> startOfTask = Optional.of(new DateTime());
        
        Iterator<Content> updatedContent = contentResolver.updatedSince(lastUpdated.get());
        
        List<Content> deleted = Lists.newArrayList();
        
        YouViewContentProcessor uploadProcessor = contentProcessor(lastUpdated.get(), Action.UPDATE);
        YouViewContentProcessor deletionProcessor = contentProcessor(lastUpdated.get(), Action.DELETE);

        while (updatedContent.hasNext()) {
            Content updated = updatedContent.next();
            if (updated.isActivelyPublished()) {
                uploadProcessor.process(updated);
                reportStatus("Uploads: " + uploadProcessor.getResult());
            } else {
                deleted.add(updated);
            }
        }
        
        List<Content> orderedForDeletion = orderContentForDeletion(deleted);

        for (Content toBeDeleted : orderedForDeletion) {
            deletionProcessor.process(toBeDeleted);
            reportStatus("Deletes: " + deletionProcessor.getResult());
        }

        reportStatus("Creating channel tasks");

        YouViewChannelProcessor channelProcessor = channelProcessor(
                Action.UPDATE,
                ChannelType.CHANNEL
        );

        YouViewChannelProcessor masterBrandProcessor = channelProcessor(
                Action.UPDATE,
                ChannelType.MASTERBRAND
        );

        ChannelQuery nitroChannelsQuery = ChannelQuery.builder()
                .withPublisher(Publisher.BBC_NITRO)
                .build();
        for (Channel channel : channelResolver.allChannels(nitroChannelsQuery)) {
            switch (channel.getChannelType()) {
            case CHANNEL:
                channelProcessor.process(channel);
                break;
            case MASTERBRAND:
                masterBrandProcessor.process(channel);
                break;
            default:
                log.warn("Unknown channel type {}", channel.getChannelType());
            }
        }

        setLastUpdatedTime(startOfTask.get());
        
        reportStatus("Uploading tasks to YouView");
        
        // temporary fix; too many txns are being generated, due to the separation of 
        // task generation and upload. Moving the upload to happen in sequence after
        // task generation should help.
        
        updateTask.run();
        
        reportStatus("Done uploading tasks to YouView");
    }
    
    private static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }
}
