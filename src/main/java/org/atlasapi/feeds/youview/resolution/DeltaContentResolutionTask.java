package org.atlasapi.feeds.youview.resolution;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.upload.granular.FilterFactory.broadcastFilter;
import static org.atlasapi.feeds.youview.upload.granular.FilterFactory.hasBeenUpdated;
import static org.atlasapi.feeds.youview.upload.granular.FilterFactory.onDemandFilter;
import static org.atlasapi.feeds.youview.upload.granular.FilterFactory.versionFilter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.HierarchicalContentOrdering;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.creation.TaskCreator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.metabroadcast.common.scheduling.ScheduledTask;


public class DeltaContentResolutionTask extends ScheduledTask {

    private static final Ordering<Content> HIERARCHICAL_ORDERING = new HierarchicalContentOrdering();
    
    private final Logger log = LoggerFactory.getLogger(DeltaContentResolutionTask.class);
    
    private final YouViewContentResolver contentResolver;
    private final YouViewLastUpdatedStore lastUpdatedStore;
    private final ContentHierarchyExpander hierarchyExpander;
    private final ContentHierarchyExtractor hierarchyExtractor;
    private final RevokedContentStore revocationStore;
    private final TaskCreator taskCreator;
    private final Publisher publisher;

    public DeltaContentResolutionTask(YouViewLastUpdatedStore lastUpdatedStore,
            ContentHierarchyExpander hierarchyExpander, ContentHierarchyExtractor hierarchyExtractor,
            Publisher publisher, YouViewContentResolver contentResolver,
            RevokedContentStore revocationStore, TaskCreator taskCreator) {
        this.contentResolver = checkNotNull(contentResolver);
        this.lastUpdatedStore = checkNotNull(lastUpdatedStore);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
        this.hierarchyExtractor = checkNotNull(hierarchyExtractor);
        this.revocationStore = checkNotNull(revocationStore);
        this.taskCreator = checkNotNull(taskCreator);
        this.publisher = checkNotNull(publisher);
    }

    @Override
    protected void runTask() {
        Optional<DateTime> lastRunTime = lastUpdatedStore.getLastUpdated(publisher);
        if (!lastRunTime.isPresent()) {
            throw new RuntimeException("Content bootstrap must be run before performing delta updates");
        }
        Iterator<Content> resolved = contentResolver.updatedSince(lastRunTime.get());
        
        Set<Content> toBeDeleted = Sets.newHashSet();
        
        while(resolved.hasNext()) {
            Content content = resolved.next();
            if (isRevoked(content)) {
                log.trace("Content {} is revoked, not uploading", content.getCanonicalUri());
                continue;
            }
            
            // split into deletes and updates
            if (isUpdate(content)) {
                expandAndUpload(content, lastRunTime);
            } else {
                Set<Content> hierarchy = expandDownwardsContentHierarchy(content); 
                toBeDeleted.addAll(hierarchy);
            }
        }
        List<Content> orderedContent = orderContentForDeletion(toBeDeleted);
        for (Content content : orderedContent) {
            // TODO this is a rather 'nuke from orbit' style approach to deletions
            expandElementsAndCreateTasks(content);
        }
    }
    
    private boolean isRevoked(Content content) {
        return revocationStore.isRevoked(content.getCanonicalUri());
    }

    // TODO if this fails early, we don't note/retry any of the elements after the failure
    private void expandAndUpload(Content content, Optional<DateTime> updatedSince) {
        if (hasBeenUpdated(content, updatedSince.get())) {
            taskCreator.create(content, Action.UPDATE);
        }
        if (content instanceof Item) {
            Map<String, ItemAndVersion> versionHierarchies = Maps.filterValues(
                    hierarchyExpander.versionHierarchiesFor((Item) content), 
                    versionFilter(updatedSince)
            );
            Map<String, ItemBroadcastHierarchy> broadcastHierarchies = Maps.filterValues(
                    hierarchyExpander.broadcastHierarchiesFor((Item) content), 
                    broadcastFilter(updatedSince)
            );
            Map<String, ItemOnDemandHierarchy> onDemandHierarchies = Maps.filterValues(
                    hierarchyExpander.onDemandHierarchiesFor((Item) content), 
                    onDemandFilter(updatedSince)
            );
            
            for (Entry<String, ItemAndVersion> version : versionHierarchies.entrySet()) {
                taskCreator.create(version.getKey(), version.getValue(), Action.UPDATE);
            }
            for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcastHierarchies.entrySet()) {
                taskCreator.create(broadcast.getKey(), broadcast.getValue(), Action.UPDATE);
            }
            for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemandHierarchies.entrySet()) {
                taskCreator.create(onDemand.getKey(), onDemand.getValue(), Action.UPDATE);
            }
        }
    }
    
    private void expandElementsAndCreateTasks(Content content) {
        taskCreator.create(content, Action.DELETE);
        if (content instanceof Item) {
            Map<String, ItemAndVersion> versionHierarchies = hierarchyExpander.versionHierarchiesFor((Item) content);
            Map<String, ItemBroadcastHierarchy> broadcastHierarchies = hierarchyExpander.broadcastHierarchiesFor((Item) content);
            Map<String, ItemOnDemandHierarchy> onDemandHierarchies = hierarchyExpander.onDemandHierarchiesFor((Item) content);
            
            for (Entry<String, ItemAndVersion> version : versionHierarchies.entrySet()) {
                taskCreator.create(version.getKey(), version.getValue(), Action.DELETE);
            }
            for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcastHierarchies.entrySet()) {
                taskCreator.create(broadcast.getKey(), broadcast.getValue(), Action.DELETE);
            }
            for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemandHierarchies.entrySet()) {
                taskCreator.create(onDemand.getKey(), onDemand.getValue(), Action.DELETE);
            }
        }
    }

    private boolean isUpdate(Content content) {
        return content.isActivelyPublished();
    }

    // TODO this is ripe for extraction to another class
    // TODO this is a simplification - this identifies candidates for deletion, but
    // does not check whether the pieces of content have already been deleted from the remote system
    private Set<Content> expandDownwardsContentHierarchy(Content content) {
        ImmutableSet.Builder<Content> hierarchy = ImmutableSet.builder();
        
        if (content instanceof Brand) {
            Iterable<Series> series = hierarchyExtractor.seriesFor((Brand) content);
            hierarchy.addAll(series);
            for (Series aSeries : series) {
                hierarchy.addAll(hierarchyExtractor.itemsFor(aSeries));
            }
        }
        if (content instanceof Series) {
            hierarchy.addAll(hierarchyExtractor.itemsFor((Series) content));
        }
        hierarchy.add(content);
        
        return hierarchy.build();
    }
    
    public static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDERING.immutableSortedCopy(toBeDeleted);
    }
}
