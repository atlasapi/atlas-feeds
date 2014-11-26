package org.atlasapi.feeds.youview.upload;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.youview.YouViewContentProcessor;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.metabroadcast.common.scheduling.UpdateProgress;

public final class DeltaUploadTask extends UploadTask {

    private static final String DELTA_STATUS_PATTERN = "Updates: processed %d of %d, %d failures. Deletes: processed %d of %d total, %d failures.";
    

    public DeltaUploadTask(YouViewClient remoteClient, LastUpdatedContentFinder contentFinder,
            YouViewLastUpdatedStore lastUpdatedStore, Publisher publisher) {
        super(remoteClient, contentFinder, lastUpdatedStore, publisher);
    }
    
    @Override
    public void runTask() {
        Optional<DateTime> lastUpdated = getLastUpdatedTime();
        if (!lastUpdated.isPresent()) {
            throw new RuntimeException("The bootstrap has not successfully run. Please run the bootstrap upload and ensure that it succeeds before running the delta upload.");
        }
        
        Iterator<Content> updatedContent = getContentSinceDate(Optional.of(lastUpdated.get()));
        
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
        UpdateProgress deletionProgress = UpdateProgress.START;
        
        YouViewContentProcessor<UpdateProgress> uploadProcessor = uploadProcessor();
        YouViewContentProcessor<UpdateProgress> deletionProcessor = deleteProcessor();
        
        for (Content content : notDeleted) {
            uploadProcessor.process(content);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProcessor.getResult(), updatesSize, deletesSize));
        }
        
        List<Content> orderedForDeletion = orderContentForDeletion(deleted);

        for (Content toBeDeleted : orderedForDeletion) {
            deletionProcessor.process(toBeDeleted);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProcessor.getResult(), updatesSize, deletesSize));
        }
        
        setLastUpdatedTime(lastUpdated.get());
        reportStatus("Complete. " + createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
    }
    
    private String createDeltaStatus(UpdateProgress updateProgress, UpdateProgress deletionProgress, int updatesSize, int deletesSize) {
        return String.format(DELTA_STATUS_PATTERN, updateProgress.getProcessed(), updatesSize, updateProgress.getFailures(), deletionProgress.getProcessed(), deletesSize, deletionProgress.getFailures());
    }

}
