package org.atlasapi.feeds.tasks.youview.creation;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.AmazonContentConsolidator;
import org.atlasapi.feeds.youview.YouviewContentMerger;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeltaTaskCreationTask extends TaskCreationTask {

    private static final Logger log = LoggerFactory.getLogger(DeltaTaskCreationTask.class);

    public static final Duration UPDATE_WINDOW_GRACE_PERIOD = Duration.standardHours(2);

    private static final Ordering<Content> HIERARCHICAL_ORDER = new HierarchicalOrdering();

    private final YouViewContentResolver contentResolver;
    private final YouviewContentMerger youviewContentMerger;
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
            ChannelResolver channelResolver,
            KnownTypeQueryExecutor mergingResolver) {
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
        checkNotNull(mergingResolver);


        this.youviewContentMerger = new YouviewContentMerger(
                mergingResolver,
                getPublisher());
    }

    @Override
    protected void runTask() {
        Optional<DateTime> lastUpdated = getLastUpdatedTime();
        if (!lastUpdated.isPresent()) {
            throw new IllegalStateException("The bootstrap for "+ getPublisher()
                                            + " has not successfully run. Please run the "
                                            + "bootstrap upload and ensure that it succeeds before "
                                            + "running the delta upload.");
        }
        
        Optional<DateTime> startOfTask = Optional.of(new DateTime());
        log.info("Started a delta YV task creation process for {} from {}", getPublisher(), lastUpdated);

        Iterator<Content> updatedContent = contentResolver.updatedSince(
                lastUpdated.get().minus(UPDATE_WINDOW_GRACE_PERIOD)
        );

        YouViewContentProcessor uploadProcessor = contentProcessor(lastUpdated.get(), Action.UPDATE);
        YouViewContentProcessor deletionProcessor = contentProcessor(lastUpdated.get(), Action.DELETE);

        List<Content> deleted;
        if(getPublisher().equals(Publisher.BBC_NITRO)){
            deleted = uploadFromBBC(updatedContent, uploadProcessor);
        }
        else if(getPublisher().equals(Publisher.AMAZON_UNBOX)){
            deleted = uploadFromAmazon(updatedContent, uploadProcessor);
        } else {
            throw new IllegalStateException("Uploading from "+getPublisher()+" to YV is not supported.");
        }

        List<Content> sortedContentForDeletion = sortContentForDeletion(deleted);
        for (Content toBeDeleted : sortedContentForDeletion) {
            deletionProcessor.process(toBeDeleted);
            reportStatus("Deletes: " + deletionProcessor.getResult());
        }

        log.info("Done creating {} tasks for YV up to {}.", getPublisher(), startOfTask.get());
        setLastUpdatedTime(startOfTask.get());

        log.info("Started uploading YV tasks from {}.", getPublisher());
        reportStatus("Uploading tasks to YouView");
        updateTask.run();

        log.info("Done uploading tasks to YV from {}", getPublisher());
        reportStatus("Done uploading tasks to YouView");
    }

    protected List<Content> uploadFromAmazon(Iterator<Content> contentPieces,
            YouViewContentProcessor uploadProcessor) {

        List<Content> deleted = Lists.newArrayList();
        while (contentPieces.hasNext()) {
            Content updatedContent = contentPieces.next();
            if (!updatedContent.isActivelyPublished()) {
                deleted.add(updatedContent);
                continue;
            }

            // YV specifically requested that we do not update episodes with episode number 0.
            // This is because to the best of our knowledge they are trailers.
            if(updatedContent instanceof Episode &&
               Objects.equals(0, ((Episode)updatedContent).getEpisodeNumber())){
                continue;
            }

            Content mergedContent;
            try {
                mergedContent = youviewContentMerger.equivAndMerge(updatedContent);
            } catch (Exception e) {
                log.error("Failed during the attempt to equiv, merge or get a repId. "
                          + "This item will not be pushed to YV. Content {}. ",
                        updatedContent.getCanonicalUri(), e);
                continue;
            }

            try {
                AmazonContentConsolidator.consolidate(mergedContent); //mutates the item
            } catch (Exception e) {
                log.error("Failed during the attempt to consolidate versions. "
                          + "This item will not be pushed to YV. Content {}. ",
                        updatedContent.getCanonicalUri(), e);
                continue;
            }

            uploadProcessor.process(mergedContent);
            reportStatus("Uploads: " + uploadProcessor.getResult());

        }
        return deleted;
    }

    private List<Content> uploadFromBBC(
            Iterator<Content> updatedContent,
            YouViewContentProcessor uploadProcessor) {

        List<Content> deleted = Lists.newArrayList();
        while (updatedContent.hasNext()) {
            Content updated = updatedContent.next();
            if (updated.isActivelyPublished()) {
                uploadProcessor.process(updated);
                reportStatus("Uploads: " + uploadProcessor.getResult());
            } else {
                deleted.add(updated);
            }
        }
        return deleted;
    }
    
    protected static List<Content> sortContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }
}
