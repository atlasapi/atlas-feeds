package org.atlasapi.feeds.youview.upload.granular;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.metabroadcast.common.scheduling.UpdateProgress;

public final class GranularDeltaUploadTask extends GranularUploadTask {

    private static final String DELTA_STATUS_PATTERN = "Updates: processed %d of %d, %d failures. Deletes: processed %d of %d total, %d failures.";
    
    private final YouViewContentResolver contentResolver;
    private final FeedStatisticsStore statsStore;
    private final Publisher publisher;
    
    public GranularDeltaUploadTask(GranularYouViewService youViewService, YouViewLastUpdatedStore lastUpdatedStore, 
            Publisher publisher, YouViewContentResolver contentResolver, ContentHierarchyExpander hierarchyExpander, 
            IdGenerator idGenerator, FeedStatisticsStore statsStore) {
        super(youViewService, lastUpdatedStore, publisher, hierarchyExpander, idGenerator);
        this.contentResolver = checkNotNull(contentResolver);
        this.statsStore = checkNotNull(statsStore);
        this.publisher = checkNotNull(publisher);
    }
    
    @Override
    public void runTask() {
        Optional<DateTime> lastUpdated = getLastUpdatedTime();
        if (!lastUpdated.isPresent()) {
            throw new RuntimeException("The bootstrap has not successfully run. Please run the bootstrap upload and ensure that it succeeds before running the delta upload.");
        }
        
        Iterator<Content> updatedContent = contentResolver.updatedSince(lastUpdated.get());
        
        Optional<DateTime> startOfTask = Optional.of(new DateTime());
        
        List<Content> deleted = Lists.newArrayList();
        List<Content> notDeleted = Lists.newArrayList();
        
        while (updatedContent.hasNext()) {
            Content updated = updatedContent.next();
            if (updated.isActivelyPublished()) {
                notDeleted.add(updated);
            } else {
                deleted.add(updated);
            }
        }
        
        int deletesSize = deleted.size();
        int updatesSize = notDeleted.size();
        int remainingCount = deletesSize + updatesSize;
        updateQueueSizeMetric(remainingCount);
        UpdateProgress deletionProgress = UpdateProgress.START;
        
        GranularYouViewContentProcessor<UpdateProgress> uploadProcessor = uploadProcessor(lastUpdated);
        GranularYouViewContentProcessor<UpdateProgress> deletionProcessor = deleteProcessor();
        
        for (Content content : notDeleted) {
            uploadProcessor.process(content);
            updateQueueSizeMetric(remainingCount--);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProcessor.getResult(), updatesSize, deletesSize));
        }
        
        List<Content> orderedForDeletion = orderContentForDeletion(deleted);

        for (Content toBeDeleted : orderedForDeletion) {
            deletionProcessor.process(toBeDeleted);
            updateQueueSizeMetric(remainingCount--);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProcessor.getResult(), updatesSize, deletesSize));
        }
        
        setLastUpdatedTime(startOfTask.get());
        reportStatus("Complete. " + createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
    }
    
    private void updateQueueSizeMetric(int queueSize) {
        statsStore.updateQueueSize(publisher, queueSize);
    }
    
    private String createDeltaStatus(UpdateProgress updateProgress, UpdateProgress deletionProgress, int updatesSize, int deletesSize) {
        return String.format(DELTA_STATUS_PATTERN, updateProgress.getProcessed(), updatesSize, updateProgress.getFailures(), deletionProgress.getProcessed(), deletesSize, deletionProgress.getFailures());
    }

}
