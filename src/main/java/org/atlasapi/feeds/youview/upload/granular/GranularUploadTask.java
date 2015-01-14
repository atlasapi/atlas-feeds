    package org.atlasapi.feeds.youview.upload.granular;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.upload.granular.FilterFactory.broadcastFilter;
import static org.atlasapi.feeds.youview.upload.granular.FilterFactory.hasBeenUpdated;
import static org.atlasapi.feeds.youview.upload.granular.FilterFactory.onDemandFilter;
import static org.atlasapi.feeds.youview.upload.granular.FilterFactory.versionFilter;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.Clock;

public abstract class GranularUploadTask extends ScheduledTask {

    private static final Ordering<Content> HIERARCHICAL_ORDER = new Ordering<Content>() {
        @Override
        public int compare(Content left, Content right) {
            if (left instanceof Item) {
                if (right instanceof Item) {
                    return 0;
                } else {
                    return 1;
                }
            } else if (left instanceof Series) {
                if (right instanceof Item) {
                    return -1;
                } else if (right instanceof Series) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (right instanceof Brand) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    };
    
    private final Logger log = LoggerFactory.getLogger(GranularUploadTask.class);

    private final GranularYouViewService youViewService;
    private final YouViewLastUpdatedStore lastUpdatedStore;
    private final Publisher publisher;
    private final ContentHierarchyExpander hierarchyExpander;
    private final IdGenerator idGenerator;
    private final FeedStatisticsStore statsStore;
    private final Clock clock;
    
    public GranularUploadTask(GranularYouViewService youViewService, YouViewLastUpdatedStore lastUpdatedStore, 
            Publisher publisher, ContentHierarchyExpander hierarchyExpander, IdGenerator idGenerator,
            FeedStatisticsStore statsStore, Clock clock) {
        this.youViewService = checkNotNull(youViewService);
        this.lastUpdatedStore = checkNotNull(lastUpdatedStore);
        this.publisher = checkNotNull(publisher);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
        this.idGenerator = checkNotNull(idGenerator);
        this.statsStore = checkNotNull(statsStore);
        this.clock = checkNotNull(clock);
    }
    
    public Optional<DateTime> getLastUpdatedTime() {
        return lastUpdatedStore.getLastUpdated(publisher);
    }
    
    public void setLastUpdatedTime(DateTime lastUpdated) {
        lastUpdatedStore.setLastUpdated(lastUpdated, publisher);
    }
    
    public static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }

    public GranularYouViewContentProcessor<UpdateProgress> uploadProcessor(final Optional<DateTime> updatedSince) {
        return new GranularYouViewContentProcessor<UpdateProgress>() {
            
            UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public boolean process(Content content) {
                try {
                    expandAndUpload(content, updatedSince);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.error("error on upload for " + content.getCanonicalUri(), e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                return shouldContinue();
            }

            @Override
            public UpdateProgress getResult() {
                return progress;
            }
        };
    }

    // TODO if this fails early, we don't note/retry any of the elements after the failure
    private void expandAndUpload(Content content, Optional<DateTime> updatedSince) {
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
                log.trace("uploading version {}", version.getKey());
                youViewService.uploadVersion(version.getValue(), version.getKey());
            }
            for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcastHierarchies.entrySet()) {
                log.trace("uploading broadcast {}", broadcast.getKey());
                youViewService.uploadBroadcast(broadcast.getValue(), broadcast.getKey());
            }
            for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemandHierarchies.entrySet()) {
                log.trace("uploading onDemand {}", onDemand.getKey());
                youViewService.uploadOnDemand(onDemand.getValue(), onDemand.getKey());
            }
        }
        if (updatedSince.isPresent()) {
            if (hasBeenUpdated(content, updatedSince.get())) {
                log.trace("uploading content {}", content.getCanonicalUri());
                youViewService.uploadContent(content);
            }
        } else {
            log.trace("uploading content {}", content.getCanonicalUri());
            youViewService.uploadContent(content);
        }
    }

    public GranularYouViewContentProcessor<UpdateProgress> deleteProcessor() {
        return new GranularYouViewContentProcessor<UpdateProgress>() {
            
            UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public boolean process(Content content) {
                try {
                    expandHierarchiesAndSendDeletes(content);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.error("error on deletion for " + content.getCanonicalUri(), e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                return shouldContinue();
            }

            @Override
            public UpdateProgress getResult() {
                return progress;
            }
        };
    }

    // TODO if this fails early, we don't note/retry any of the elements after the failure
    private void expandHierarchiesAndSendDeletes(Content content) {
        if (content instanceof Item) {
            Map<String, ItemAndVersion> versionHierarchies = hierarchyExpander.versionHierarchiesFor((Item) content);
            for (Entry<String, ItemAndVersion> version : versionHierarchies.entrySet()) {
                youViewService.sendDeleteFor(content, TVAElementType.VERSION, version.getKey());
            }
            Map<String, ItemBroadcastHierarchy> broadcastHierarchies = hierarchyExpander.broadcastHierarchiesFor((Item) content);
            for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcastHierarchies.entrySet()) {
                youViewService.sendDeleteFor(content, TVAElementType.BROADCAST, broadcast.getKey());
            }
            Map<String, ItemOnDemandHierarchy> onDemandHierarchies = hierarchyExpander.onDemandHierarchiesFor((Item) content);
            for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemandHierarchies.entrySet()) {
                youViewService.sendDeleteFor(content, TVAElementType.ONDEMAND, onDemand.getKey());
            }
        }
        youViewService.sendDeleteFor(content, determineType(content), idGenerator.generateContentCrid(content));
    }

    private TVAElementType determineType(Content content) {
        if (content instanceof Brand) {
            return TVAElementType.BRAND;
        }
        if (content instanceof Series) {
            return TVAElementType.SERIES;
        }
        // TODO this is crude
        return TVAElementType.ITEM;
    }

    // TODO this should use the transaction upload time
    public void updateFeedStatistics(int queueSize, Content content) {
        statsStore.updateFeedStatistics(publisher, queueSize, calculateLatency(clock.now(), content));
    }
    
    private Duration calculateLatency(final DateTime uploadTime, Content content) {
        return new Duration(uploadTime, content.getLastUpdated());
    }
}
