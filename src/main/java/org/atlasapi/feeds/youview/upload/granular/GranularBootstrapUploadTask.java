package org.atlasapi.feeds.youview.upload.granular;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.feeds.youview.statistics.FeedStatistics;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.Clock;

public final class GranularBootstrapUploadTask extends GranularUploadTask {
    
    private static final Predicate<Content> IS_ACTIVELY_PUBLISHED = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return input.isActivelyPublished();
        }
    };
    private final YouViewContentResolver contentResolver;
    private final FeedStatisticsStore statsStore;
    private final Clock clock;
    private final Publisher publisher;
    
    public GranularBootstrapUploadTask(GranularYouViewService youViewService, YouViewLastUpdatedStore lastUpdatedStore, 
            Publisher publisher, YouViewContentResolver contentResolver, ContentHierarchyExpander hierarchyExpander, 
            IdGenerator idGenerator, FeedStatisticsStore statsStore, Clock clock) {
        super(youViewService, lastUpdatedStore, publisher, hierarchyExpander, idGenerator);
        this.contentResolver = checkNotNull(contentResolver);
        this.statsStore = checkNotNull(statsStore);
        this.clock = checkNotNull(clock);
        this.publisher = checkNotNull(publisher);
    }
    
    @Override
    public void runTask() {
        DateTime lastUpdated = new DateTime();
        Iterator<Content> allContent = contentResolver.allContent();
        
        int queueSize = Iterators.size(allContent);
        Duration updateLatency = Duration.ZERO;
        statsStore.save(createFeedStatistics(queueSize, updateLatency));
        
        GranularYouViewContentProcessor<UpdateProgress> processor = uploadProcessor(Optional.<DateTime>absent());
        
        while (allContent.hasNext()) {
            if (!shouldContinue()) {
                return;
            }
            Content next = allContent.next();
            if (IS_ACTIVELY_PUBLISHED.apply(next)) {
                processor.process(next);
                // this time is a bit of a hack, should use the actual upload time, but don't have txn here
                // TODO reconsider this during refactor
                statsStore.updateAverageLatency(publisher, calculateLatency(clock.now(), next));
                reportStatus(processor.getResult().toString());
            }
        }

        setLastUpdatedTime(lastUpdated);
        reportStatus(processor.getResult().toString());
    }
    
    private FeedStatistics createFeedStatistics(int queueSize, Duration updateLatency) {
        // uptime metric is unimportant, it's reset when resolved
        return new FeedStatistics(publisher, queueSize, updateLatency, new DateTime());
    }
    
    protected Duration calculateLatency(final DateTime uploadTime, Content content) {
        return new Duration(uploadTime, content.getLastUpdated());
    }
}
