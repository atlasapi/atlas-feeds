    package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.youview.YouViewContentProcessor;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.DateTimeZones;

public abstract class UploadTask extends ScheduledTask {

    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);
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
    
    private final Logger log = LoggerFactory.getLogger(UploadTask.class);

    private final LastUpdatedContentFinder contentFinder;
    private final YouViewService remoteClient;
    private final YouViewLastUpdatedStore lastUpdatedStore;
    private final Publisher publisher;
    
    public UploadTask(YouViewService remoteClient, LastUpdatedContentFinder contentFinder,
            YouViewLastUpdatedStore lastUpdatedStore, Publisher publisher) {
        this.remoteClient = checkNotNull(remoteClient);
        this.contentFinder = checkNotNull(contentFinder);
        this.lastUpdatedStore = checkNotNull(lastUpdatedStore);
        this.publisher = checkNotNull(publisher);
    }
    
    public Optional<DateTime> getLastUpdatedTime() {
        return lastUpdatedStore.getLastUpdated(publisher);
    }
    
    public void setLastUpdatedTime(DateTime lastUpdated) {
        lastUpdatedStore.setLastUpdated(lastUpdated, publisher);
    }
    
    public Iterator<Content> getContentSinceDate(Optional<DateTime> since) {
        DateTime start = since.isPresent() ? since.get() : START_OF_TIME;
        return contentFinder.updatedSince(publisher, start);
    }

    public static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }

    public YouViewContentProcessor<UpdateProgress> uploadProcessor() {
        return new YouViewContentProcessor<UpdateProgress>() {
            
            UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public boolean process(Content content) {
                try {
                    remoteClient.upload(content);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.error("error on upload for " + content.getCanonicalUri() + " : " + e);
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

    public YouViewContentProcessor<UpdateProgress> deleteProcessor() {
        return new YouViewContentProcessor<UpdateProgress>() {
            
            UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public boolean process(Content content) {
                try {
                    remoteClient.sendDeleteFor(content);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.error("error on deletion for " + content.getCanonicalUri() + " : " + e);
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
