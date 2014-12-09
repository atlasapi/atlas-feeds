package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.Clock;

public final class DeltaUploadTask extends UploadTask {

    private static final String DELTA_STATUS_PATTERN = "Updates: processed %d of %d, %d failures. Deletes: processed %d of %d total, %d failures.";

    private final YouViewContentResolver contentResolver;

    public DeltaUploadTask(YouViewService remoteClient, YouViewLastUpdatedStore lastUpdatedStore, 
            Publisher publisher, YouViewContentResolver contentResolver, FeedStatisticsStore statsStore,
            Clock clock) {
        super(remoteClient, lastUpdatedStore, publisher, statsStore, clock);
        this.contentResolver = checkNotNull(contentResolver);
    }
    
    @Override
    public void runTask() {
        Optional<DateTime> lastUpdated = getLastUpdatedTime();
        if (!lastUpdated.isPresent()) {
            throw new RuntimeException("The bootstrap has not successfully run. Please run the bootstrap upload and ensure that it succeeds before running the delta upload.");
        }
        
        Iterator<Content> updatedContent = contentResolver.updatedSince(lastUpdated.get());
        
        lastUpdated = Optional.of(new DateTime());
        
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
        UpdateProgress deletionProgress = UpdateProgress.START;
        
        YouViewContentProcessor<UpdateProgress> uploadProcessor = uploadProcessor();
        YouViewContentProcessor<UpdateProgress> deletionProcessor = deleteProcessor();
        
        for (Content content : notDeleted) {
            uploadProcessor.process(content);
            updateFeedStatistics(remainingCount--, content);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProcessor.getResult(), updatesSize, deletesSize));
        }
        
        List<Content> orderedForDeletion = orderContentForDeletion(deleted);

        for (Content toBeDeleted : orderedForDeletion) {
            deletionProcessor.process(toBeDeleted);
            updateFeedStatistics(remainingCount--, toBeDeleted);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProcessor.getResult(), updatesSize, deletesSize));
        }
        
        setLastUpdatedTime(lastUpdated.get());
        reportStatus("Complete. " + createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
    }
    
    private String createDeltaStatus(UpdateProgress updateProgress, UpdateProgress deletionProgress, int updatesSize, int deletesSize) {
        return String.format(DELTA_STATUS_PATTERN, updateProgress.getProcessed(), updatesSize, updateProgress.getFailures(), deletionProgress.getProcessed(), deletesSize, deletionProgress.getFailures());
    }

}
