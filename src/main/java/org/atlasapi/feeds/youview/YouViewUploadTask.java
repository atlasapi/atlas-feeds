package org.atlasapi.feeds.youview;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.utils.UpdateProgress;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.content.Content;
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
import com.metabroadcast.common.time.DateTimeZones;

public class YouViewUploadTask extends ScheduledTask {

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
    private final YouViewUploader uploader;
    private final YouViewDeleter deleter;
    private final boolean isBootstrap;
    private final int chunkSize;
    
    private final Logger log = LoggerFactory.getLogger(YouViewUploadTask.class);
    
    public YouViewUploadTask(YouViewUploader uploader, YouViewDeleter deleter, int chunkSize, LastUpdatedContentFinder contentFinder, YouViewLastUpdatedStore store, boolean isBootstrap) {
        this.uploader = uploader;
        this.deleter = deleter;
        this.chunkSize = chunkSize;
        this.contentFinder = contentFinder;
        this.store = store;
        this.isBootstrap = isBootstrap;
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
        Optional<DateTime> lastUpdated = store.getLastUpdated();
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
        
        YouViewUploadProcessor<UpdateProgress> uploadProcessor = uploadProcessor();
        for (Iterable<Content> chunk : Iterables.partition(notDeleted, chunkSize)) {
            uploadProcessor.process(chunk);
        }
        
        int successes = deleter.sendDeletes(deleted);
        store.setLastUpdated(lastUpdated.get());
        
        reportStatus(String.format("Deletes: %d succeeded of %d total", successes, Iterables.size(deleted)));
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
        }

        store.setLastUpdated(lastUpdated);
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
                    uploader.upload(chunk);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.error("error on chunk upload: " + e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                reportStatus(progress.toString());
                return shouldContinue();
            }

            @Override
            public UpdateProgress getResult() {
                return progress;
            }
        };
    }
}
