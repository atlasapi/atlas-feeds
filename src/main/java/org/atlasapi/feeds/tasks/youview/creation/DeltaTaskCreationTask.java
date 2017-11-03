package org.atlasapi.feeds.tasks.youview.creation;

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;

import org.atlasapi.application.query.ApplicationNotFoundException;
import org.atlasapi.application.query.InvalidApiKeyException;
import org.atlasapi.equiv.OutputContentMerger;
import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.applications.client.ApplicationsClient;
import com.metabroadcast.applications.client.ApplicationsClientImpl;
import com.metabroadcast.applications.client.exceptions.ErrorCode;
import com.metabroadcast.applications.client.metric.Metrics;
import com.metabroadcast.applications.client.model.internal.AccessRoles;
import com.metabroadcast.applications.client.model.internal.Application;
import com.metabroadcast.applications.client.model.internal.ApplicationConfiguration;
import com.metabroadcast.applications.client.model.internal.Environment;
import com.metabroadcast.applications.client.query.Query;
import com.metabroadcast.applications.client.query.Result;
import com.metabroadcast.applications.client.service.HttpServiceClient;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
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
            throw new IllegalStateException("The bootstrap has not successfully run. Please run the "
                                            + "bootstrap upload and ensure that it succeeds before "
                                            + "running the delta upload.");
        }
        
        Optional<DateTime> startOfTask = Optional.of(new DateTime());
        log.info("@@@ Started a new delta run for "+getPublisherString());

        Iterator<Content> updatedContent = contentResolver.updatedSince(
                lastUpdated.get().minus(UPDATE_WINDOW_GRACE_PERIOD)
        );

        ApplicationsClient applicationsClient = ApplicationsClientImpl.create("http://applications-service.production.svc.cluster.local", new MetricRegistry());

        String apiKey = "fb0762e32f6041c4b9ef9f68bd22da14";
        java.util.Optional<Application> application;
        try {

            Result result = applicationsClient
                    .resolve(Query.create(apiKey, Environment.PROD));
            if (result.getErrorCode().isPresent()) {
                throw InvalidApiKeyException.create(apiKey, result.getErrorCode().orElse(null));
            } else {
                application = result.getSingleResult();
            }

        } catch (InvalidApiKeyException e) {
            log.error("Problem with API key for request", e);
            application = java.util.Optional.empty();
        }

        if (!application.isPresent()) {
            log.error("No application found for request");
        }



        OutputContentMerger contentMerger = new OutputContentMerger();
        List<Content> mergedContent = contentMerger.merge(application.get(), Lists.newArrayList(updatedContent));

        List<Content> deleted = Lists.newArrayList();
        YouViewContentProcessor uploadProcessor = contentProcessor(lastUpdated.get(), Action.UPDATE);
        YouViewContentProcessor deletionProcessor = contentProcessor(lastUpdated.get(), Action.DELETE);

        int deletingContent= 0;
        while (updatedContent.hasNext()) {
            Content updated = updatedContent.next();
            //Update the content with a representative ID
            updated.setId(getRepIdClient().getDecoded(updated.getId()));
            if (updated.isActivelyPublished()) {
                uploadProcessor.process(updated);
                reportStatus("Uploads: " + uploadProcessor.getResult());
            } else {
                deleted.add(updated);
            }
        }
        
        List<Content> orderedForDeletion = orderContentForDeletion(deleted);

        for (Content toBeDeleted : orderedForDeletion) {
            log.info("@@@ processing content "+toBeDeleted.getId()+"to be DELETED no:"+ ++deletingContent +getPublisherString());
            deletionProcessor.process(toBeDeleted);
            reportStatus("Deletes: " + deletionProcessor.getResult());
        }

        log.info("@@@"+getPublisherString()+" setting last update time to "+startOfTask.get());
        setLastUpdatedTime(startOfTask.get());

        log.info("@@@"+getPublisherString()+" Starting the upload task.");
        reportStatus("Uploading tasks to YouView");
        updateTask.run();

        log.info("@@@"+getPublisherString()+" Done");
        reportStatus("Done uploading tasks to YouView");
    }
    
    private static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }
}
