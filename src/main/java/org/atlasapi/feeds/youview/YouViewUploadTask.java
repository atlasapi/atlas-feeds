    package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.upload.YouViewRemoteClient.orderContentForDeletion;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.DateTimeZones;

public class YouViewUploadTask extends ScheduledTask {

    private static final String DELTA_STATUS_PATTERN = "Updates: processed %d of %d, %d failures. Deletes: processed %d of %d total, %d failures.";
    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);
    private static final Publisher PUBLISHER = Publisher.LOVEFILM;
    private static final Predicate<Content> IS_ACTIVELY_PUBLISHED = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return input.isActivelyPublished();
        }
    };
    
    private final LastUpdatedContentFinder contentFinder;
    private final YouViewLastUpdatedStore store;
    private final boolean isBootstrap;
    private final int chunkSize;
    
    private final Logger log = LoggerFactory.getLogger(YouViewUploadTask.class);
    private final Publisher publisher;
    private final YouViewRemoteClient remoteClient;
    private final TransactionStore transactionStore;
    
    public YouViewUploadTask(YouViewRemoteClient remoteClient, int chunkSize, LastUpdatedContentFinder contentFinder, 
            YouViewLastUpdatedStore store, Publisher publisher, boolean isBootstrap, TransactionStore transactionStore) {
        this.remoteClient = checkNotNull(remoteClient);
        this.chunkSize = checkNotNull(chunkSize);
        this.contentFinder = checkNotNull(contentFinder);
        this.store = checkNotNull(store);
        this.publisher = checkNotNull(publisher);
        this.isBootstrap = checkNotNull(isBootstrap);
        this.transactionStore = checkNotNull(transactionStore);
    }
    
    @Override
    protected void runTask() {
        if (isBootstrap) {
            runBootstrap();
        } else {
            runDelta();
        }
    }

    public void runDelta() {
        Optional<DateTime> lastUpdated = store.getLastUpdated(publisher);
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
        
        YouViewUploadProcessor<UpdateProgress> uploadProcessor = uploadProcessor();
        for (Iterable<Content> chunk : Iterables.partition(notDeleted, chunkSize)) {
            uploadProcessor.process(chunk);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
        }
        
        List<Content> orderedForDeletion = orderContentForDeletion(deleted);

        for (Content toBeDeleted : orderedForDeletion) {
            if (remoteClient.sendDeleteFor(toBeDeleted)) {
                deletionProgress = deletionProgress.reduce(UpdateProgress.SUCCESS);
            } else {
                deletionProgress = deletionProgress.reduce(UpdateProgress.FAILURE);
            }
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
        }
        
        store.setLastUpdated(lastUpdated.get(), publisher);
        reportStatus("Complete. " + createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
    }
    
    private String createDeltaStatus(UpdateProgress updateProgress, UpdateProgress deletionProgress, int updatesSize, int deletesSize) {
        return String.format(DELTA_STATUS_PATTERN, updateProgress.getProcessed(), updatesSize, updateProgress.getFailures(), deletionProgress.getProcessed(), deletesSize, deletionProgress.getFailures());
    }

    public void runBootstrap() {
        DateTime lastUpdated = new DateTime();
        Iterator<Content> allContent = getContentSinceDate(Optional.<DateTime>absent());
        
        YouViewUploadProcessor<UpdateProgress> processor = uploadProcessor();
        
        while (allContent.hasNext()) {
            if (!shouldContinue()) {
                return;
            }
            Builder<Content> chunk = ImmutableList.builder();
            int chunkCount = 0;
            while (chunkCount < chunkSize && allContent.hasNext()) {
                Content next = allContent.next();
                if (IS_ACTIVELY_PUBLISHED.apply(next)) {
                    chunk.add(next);
                    chunkCount++;
                }
            }
            processor.process(chunk.build());
            reportStatus(processor.getResult().toString());
        }

        store.setLastUpdated(lastUpdated, publisher);
        reportStatus(processor.getResult().toString());
    }
    
    private Iterator<Content> getContentSinceDate(Optional<DateTime> since) {
        DateTime start = since.isPresent() ? since.get() : START_OF_TIME;
        return contentFinder.updatedSince(PUBLISHER, start);
    }

    private YouViewUploadProcessor<UpdateProgress> uploadProcessor() {
        return new YouViewUploadProcessor<UpdateProgress>() {
            
            UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public boolean process(Iterable<Content> chunk) {
                try {
                    Optional<Transaction> transaction = remoteClient.upload(chunk);
                    if (transaction.isPresent()) {
                        transactionStore.save(transaction.get());
                    }
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.error("error on chunk upload: " + e);
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
}
