package org.atlasapi.feeds.tasks.youview.creation;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.PerPublisherConfig;
import org.atlasapi.feeds.youview.YouviewContentMerger;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;

import com.metabroadcast.applications.client.metric.Metrics;
import com.metabroadcast.applications.client.model.internal.Application;
import com.metabroadcast.applications.client.service.HttpServiceClient;
import com.metabroadcast.applications.client.translators.ServiceModelTranslator;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeltaTaskCreationTask extends TaskCreationTask {

    private static final Logger log = LoggerFactory.getLogger(TaskCreationTask.class);

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
            KnownTypeQueryExecutor mergingResolver,
            HttpServiceClient applicationClient) {
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
        //Get the app ID from the configuration, and translate it to a usable application
        String id = PerPublisherConfig.TO_APP_ID_MAP.get(publisher);
        com.metabroadcast.applications.client.model.service.Application serviceApp
                = applicationClient.resolve(id);
        ServiceModelTranslator translator =
                ServiceModelTranslator.create(Metrics.create(new MetricRegistry()));
        Application internalApp = translator.translate(serviceApp);

        this.youviewContentMerger = new YouviewContentMerger(
                mergingResolver,
                getPublisher(),
                internalApp);
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

        List<Content> orderedForDeletion = orderContentForDeletion(deleted);
        int deletingContent= 0;
        for (Content toBeDeleted : orderedForDeletion) {
            log.info("@@@ processing content " + toBeDeleted.getId() + "to be DELETED no:" + ++deletingContent + getPublisher());
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

    private List<Content> uploadFromAmazon(Iterator<Content> contentPieces,
            YouViewContentProcessor uploadProcessor) {

        List<Content> deleted = Lists.newArrayList();
        while (contentPieces.hasNext()) {
            Content updatedContent = contentPieces.next();
            if (updatedContent.isActivelyPublished()) {
                Content mergedContent;
                try {
                    mergedContent = youviewContentMerger.equivAndMerge(updatedContent);
                } catch (Exception e) {
                    log.error("Uploading {} from amazon failed during the attempt to "
                              + "equiv, merge and get a repId.", updatedContent.getId(), e);
                    continue;
                }

                uploadProcessor.process(mergedContent);
                reportStatus("Uploads: " + uploadProcessor.getResult());
            } else {
                deleted.add(updatedContent);
            }
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
    
    private static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }
}
