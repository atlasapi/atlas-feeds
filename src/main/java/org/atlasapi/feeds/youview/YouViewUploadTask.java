    package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.upload.HttpYouViewClient.orderContentForDeletion;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.statistics.FeedStatistics;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.feeds.youview.upload.YouViewClient;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.DateTimeZones;

public class YouViewUploadTask extends ScheduledTask {

    private static final String DELTA_STATUS_PATTERN = "Updates: processed %d of %d, %d failures. Deletes: processed %d of %d total, %d failures.";
    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);
    private static final Predicate<Content> IS_ACTIVELY_PUBLISHED = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return input.isActivelyPublished();
        }
    };
    
    private final Logger log = LoggerFactory.getLogger(YouViewUploadTask.class);

    private final LastUpdatedContentFinder contentFinder;
    private final YouViewLastUpdatedStore store;
    private final boolean isBootstrap;
    private final Publisher publisher;
    private final FeedStatisticsStore statsStore;
    private final YouViewClient remoteClient;
    
    public YouViewUploadTask(YouViewClient remoteClient, LastUpdatedContentFinder contentFinder, 
            YouViewLastUpdatedStore store, Publisher publisher, boolean isBootstrap, FeedStatisticsStore statsStore) {
        this.remoteClient = checkNotNull(remoteClient);
        this.contentFinder = checkNotNull(contentFinder);
        this.store = checkNotNull(store);
        this.publisher = checkNotNull(publisher);
        this.isBootstrap = checkNotNull(isBootstrap);
        this.statsStore = checkNotNull(statsStore);
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
        int remainingCount = deletesSize + updatesSize;
        updateQueueSizeMetric(remainingCount);
        UpdateProgress deletionProgress = UpdateProgress.START;
        
        YouViewUploadProcessor<UpdateProgress> uploadProcessor = uploadProcessor();
        for (Content content : notDeleted) {
            uploadProcessor.process(content);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
            updateQueueSizeMetric(remainingCount--);
        }
        
        List<Content> orderedForDeletion = orderContentForDeletion(deleted);

        for (Content toBeDeleted : orderedForDeletion) {
            remoteClient.sendDeleteFor(toBeDeleted);
            deletionProgress = deletionProgress.reduce(UpdateProgress.SUCCESS);
            remoteClient.sendDeleteFor(toBeDeleted);
            reportStatus(createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
            updateQueueSizeMetric(remainingCount--);
        }
        
        store.setLastUpdated(lastUpdated.get(), publisher);
        reportStatus("Complete. " + createDeltaStatus(uploadProcessor.getResult(), deletionProgress, updatesSize, deletesSize));
    }

    private void updateQueueSizeMetric(int queueSize) {
        // TODO publisher is hardcoded for now
        statsStore.updateQueueSize(Publisher.BBC_NITRO, queueSize);
    }
    
    private String createDeltaStatus(UpdateProgress updateProgress, UpdateProgress deletionProgress, int updatesSize, int deletesSize) {
        return String.format(DELTA_STATUS_PATTERN, updateProgress.getProcessed(), updatesSize, updateProgress.getFailures(), deletionProgress.getProcessed(), deletesSize, deletionProgress.getFailures());
    }

    public void runBootstrap() {
        DateTime lastUpdated = new DateTime();
        Iterator<Content> allContent = getContentSinceDate(Optional.<DateTime>absent());
        
        // TODO THIS IS SO HORRIFIC IT'S NOT EVEN TRUE
        int queueSize = Iterators.size(allContent);
        Duration updateLatency = Duration.millis(0l);
        statsStore.save(createFeedStatistics(queueSize, updateLatency));
        
        YouViewUploadProcessor<UpdateProgress> processor = uploadProcessor();
        
        while (allContent.hasNext()) {
            if (!shouldContinue()) {
                return;
            }
            Content next = allContent.next();
            if (IS_ACTIVELY_PUBLISHED.apply(next)) {
                processor.process(next);
                reportStatus(processor.getResult().toString());
            }
            queueSize--;
        }

        store.setLastUpdated(lastUpdated, publisher);
        reportStatus(processor.getResult().toString());
    }

    private FeedStatistics createFeedStatistics(int queueSize, Duration updateLatency) {
        // uptime metric is unimportant, it's reset when resolved
        return new FeedStatistics(Publisher.BBC_NITRO, queueSize, updateLatency, new DateTime());
    }
    
    private Iterator<Content> getContentSinceDate(Optional<DateTime> since) {
        DateTime start = since.isPresent() ? since.get() : START_OF_TIME;
        return contentFinder.updatedSince(publisher, start);
    }

    private YouViewUploadProcessor<UpdateProgress> uploadProcessor() {
        return new YouViewUploadProcessor<UpdateProgress>() {
            
            UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public boolean process(Content content) {
                try {
//                    Transaction transaction = remoteClient.upload(content);
//                    transactionStore.save(transaction);
                    remoteClient.upload(content);
                    // TODO plumb in feed stats store properly here
                    statsStore.updateAverageLatency(Publisher.BBC_NITRO, calculateLatency(new DateTime(), content));
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

    protected Duration calculateLatency(final DateTime uploadTime, Content content) {
        return new Duration(uploadTime, content.getLastFetched());
    }
}
