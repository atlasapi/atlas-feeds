package org.atlasapi.feeds.tasks.youview.creation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.DeleteTask;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.YouviewContentMerger;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.feeds.youview.unbox.AmazonContentConsolidator;
import org.atlasapi.feeds.youview.unbox.AmazonIdGenerator;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.lookup.entry.LookupEntry;
import org.atlasapi.persistence.lookup.entry.LookupEntryStore;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeltaTaskCreationTask extends TaskCreationTask {

    private static final Logger log = LoggerFactory.getLogger(DeltaTaskCreationTask.class);

    public static final Duration UPDATE_WINDOW_GRACE_PERIOD = Duration.standardHours(2);

    private static final String GB_AMAZON_ASIN = "gb:amazon:asin";
    private static final String URI_PREFIX = "http://unbox.amazon.co.uk/";

    private static final Ordering<Content> HIERARCHICAL_ORDER = HierarchicalOrdering.create();

    protected final YouViewContentResolver contentResolver;
    private final YouviewContentMerger youviewContentMerger;
    private final ChannelResolver channelResolver;
    private final UpdateTask updateTask;
    private final DeleteTask deleteTask;
    private final LookupEntryStore lookupEntryStore;

    public DeltaTaskCreationTask(
            YouViewLastUpdatedStore lastUpdatedStore,
            Publisher publisher,
            ContentHierarchyExpander hierarchyExpander,
            IdGenerator idGenerator,
            TaskStore taskStore,
            TaskCreator taskCreator,
            PayloadCreator payloadCreator,
            UpdateTask updateTask,
            DeleteTask deleteTask,
            YouViewContentResolver contentResolver,
            YouViewPayloadHashStore payloadHashStore,
            ChannelResolver channelResolver,
            KnownTypeQueryExecutor mergingResolver,
            LookupEntryStore lookupEntryStore
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
        this.deleteTask = checkNotNull(deleteTask);
        checkNotNull(mergingResolver);


        this.youviewContentMerger = new YouviewContentMerger(
                mergingResolver,
                getPublisher());

        this.lookupEntryStore = checkNotNull(lookupEntryStore);
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
        
        java.util.Optional<DateTime> startOfTask = java.util.Optional.of(new DateTime());

        Iterator<Content> updatedContent;
        DateTime from;
        DateTime to;

        if (getPublisher().equals(Publisher.AMAZON_UNBOX)) {
            from = lastUpdated.get().minus(UPDATE_WINDOW_GRACE_PERIOD);
            to = startOfTask.get().isBefore(from.plusMonths(3))
                    ? startOfTask.get()
                    : from.plusMonths(3);
            updatedContent = contentResolver.updatedBetween(from, to);
//            updatedContent = appendEquivalenceChanges(lastUpdated, updatedContent);

            log.info("Started a delta YV task creation process for {} from {} to {}",
                    getPublisher(), from, to);
        } else {
            from = lastUpdated.get().minus(UPDATE_WINDOW_GRACE_PERIOD);
            to = lastUpdated.get().plusHours(1);
            updatedContent = contentResolver.updatedSince(from);
            log.info("Started a delta YV task creation process for {} from {}",
                    getPublisher(), lastUpdated);
        }

        YouViewContentProcessor uploadProcessor = contentProcessor(lastUpdated.get(), Action.UPDATE);
        YouViewContentProcessor deletionProcessor = contentProcessor(lastUpdated.get(), Action.DELETE);

        Set<Content> forDeletion;
        if(getPublisher().equals(Publisher.BBC_NITRO)){
            forDeletion = uploadFromBBC(updatedContent, uploadProcessor);
        }
        else if(getPublisher().equals(Publisher.AMAZON_UNBOX)){
            forDeletion = uploadFromAmazon(updatedContent, uploadProcessor);
        } else {
            throw new IllegalStateException("Uploading from "+getPublisher()+" to YV is not supported.");
        }

        Set<Content> sortedContentForDeletion = ImmutableSortedSet.copyOf(HIERARCHICAL_ORDER, forDeletion);
        for (Content toBeDeleted : sortedContentForDeletion) {
            deletionProcessor.process(toBeDeleted);
            reportStatus("Deletes: " + deletionProcessor.getResult());
        }

        log.info("Done creating {} tasks for YV up to {}.", getPublisher(), startOfTask.get());
        setLastUpdatedTime(to);

        log.info("Started uploading YV tasks from {}.", getPublisher());
        reportStatus("Uploading tasks to YouView");

        // temporarily disable AMAZON uploads & deletes (commented out below)
        if(!getPublisher().equals(Publisher.AMAZON_UNBOX)) {
            updateTask.run();
        }
//        if(getPublisher().equals(Publisher.AMAZON_UNBOX)){ //bbc deletes run on schedule. Don't know why, don't wanna mess with it, doubt it would be a problem.
//            deleteTask.run();
//        }

        log.info("Done uploading tasks to YV from {}", getPublisher());
        reportStatus("Done uploading tasks to YouView");
    }

    private Iterator<Content> appendEquivalenceChanges(Optional<DateTime> lastUpdated,
            Iterator<Content> updatedContent) {

            Iterable<LookupEntry> updatedAmazonSet = lookupEntryStore.updatedSince(
                    Publisher.AMAZON_UNBOX,
                    lastUpdated.get().minus(UPDATE_WINDOW_GRACE_PERIOD));

            Set<Long> resolvedIds = Sets.newHashSet(updatedContent).stream()
                    .map(Identified::getId)
                    .collect(Collectors.toSet());

            ResolvedContent resolved = contentResolver.findByUris(
                    StreamSupport.stream(updatedAmazonSet.spliterator(), false)
                            //filter out the id's we already have.
                            .filter(entry -> !resolvedIds.contains(entry.id()))
                            .map(LookupEntry::uri)
                            .collect(Collectors.toList())
            );

            return Iterators.concat(updatedContent, translateResolvedToContent(resolved));
    }

    //Returns the content that should be deleted.
    protected Set<Content> uploadFromAmazon(
            Iterator<? extends Content> contentPieces,
            YouViewContentProcessor uploadProcessor
    ) {
        Set<Content> forDeletion = Sets.newLinkedHashSet();
        while (contentPieces.hasNext()) {
            Content updatedContent = contentPieces.next();
            if (!updatedContent.isActivelyPublished()) {
                forDeletion.add(updatedContent);
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
                forDeletion.addAll(extractForDeletion(mergedContent));
            } catch (Exception e) {
                log.error("Failed during the attempt to consolidate versions. "
                          + "This item will not be pushed to YV. Content {}. ",
                        updatedContent.getCanonicalUri(), e);
                continue;
            }

            uploadProcessor.process(mergedContent);
            reportStatus("Uploads: " + uploadProcessor.getResult());

        }
        return forDeletion;
    }

    private Set<Content> extractForDeletion(Content mergedContent) {
        //Good, now that we have the merged content, we need to ensure that the pieces this
        //represents are not uploaded to YV separately. One case that can cause this is that
        //content was uploaded to YV before it had a chance to equivalate, and thus each
        //piece was uploaded separately.
        Set<String> contributingUris = getContributingAsins(mergedContent);

        ResolvedContent resolved = contentResolver.findByUris(contributingUris);
        if (resolved != null && resolved.getAllResolvedResults() != null) {
            return resolved.getAllResolvedResults().stream()
                     .filter(c -> c instanceof Content)
                     .map(c -> (Content) c)
                     .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    //does not include self.
    public static Set<String> getContributingAsins(Content mergedContent) {
        //find all URIs that took part in the merge
        String ourAsin = AmazonIdGenerator.getAsin(mergedContent);
        Set<Alias> aliases = mergedContent.getAliases();
        Set<String> contributingUris = null;
        if(aliases != null){
            contributingUris = aliases.stream()
                    .filter(c -> c.getNamespace().equals(GB_AMAZON_ASIN))
                    .filter(c -> !c.getValue().equals(ourAsin))
                    .map(Alias::getValue)
                    .map(c-> URI_PREFIX + c)
                    .collect(Collectors.toSet());
        }
        return contributingUris;
    }

    //returns the content that should be deleted.
    private Set<Content> uploadFromBBC(
            Iterator<Content> updatedContent,
            YouViewContentProcessor uploadProcessor) {

        Set<Content> deleted = Sets.newLinkedHashSet();
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

    private Iterator<Content> translateResolvedToContent(ResolvedContent resolvedContent) {
        if (resolvedContent.isEmpty()) {
            Set<Content> nothing = Sets.newHashSet();
            return nothing.iterator();
        }

        ImmutableList.Builder<Content> contentBuilder = ImmutableList.builder();

        resolvedContent.getAllResolvedResults().stream()
                .filter(res -> ((Content) res).getMediaType().equals(MediaType.VIDEO))
                .forEach(
                        res -> {
                            if (res instanceof Episode) {
                                contentBuilder.add((Episode) res);
                            } else if (res instanceof Series) {
                                contentBuilder.add((Series) res);
                            } else if (res instanceof Brand) {
                                contentBuilder.add((Brand) res);
                            } else if (res instanceof Item) {
                                contentBuilder.add((Item) res);
                            } else if (res instanceof Container) {
                                contentBuilder.add((Container) res);
                            } else {
                                contentBuilder.add((Content) res);
                            }
                        });

        return contentBuilder.build().iterator();
    }
}
