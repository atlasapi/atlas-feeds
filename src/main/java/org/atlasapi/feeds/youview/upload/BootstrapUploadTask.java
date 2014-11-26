package org.atlasapi.feeds.youview.upload;

import java.util.Iterator;

import org.atlasapi.feeds.youview.YouViewContentProcessor;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.metabroadcast.common.scheduling.UpdateProgress;

public final class BootstrapUploadTask extends UploadTask {
    
    private static final Predicate<Content> IS_ACTIVELY_PUBLISHED = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return input.isActivelyPublished();
        }
    };
    
    public BootstrapUploadTask(YouViewClient remoteClient, LastUpdatedContentFinder contentFinder,
            YouViewLastUpdatedStore lastUpdatedStore, Publisher publisher) {
        super(remoteClient, contentFinder, lastUpdatedStore, publisher);
    }
    
    @Override
    public void runTask() {
        DateTime lastUpdated = new DateTime();
        Iterator<Content> allContent = getContentSinceDate(Optional.<DateTime>absent());
        
        YouViewContentProcessor<UpdateProgress> processor = uploadProcessor();
        
        while (allContent.hasNext()) {
            if (!shouldContinue()) {
                return;
            }
            Content next = allContent.next();
            if (IS_ACTIVELY_PUBLISHED.apply(next)) {
                processor.process(next);
                reportStatus(processor.getResult().toString());
            }
        }

        setLastUpdatedTime(lastUpdated);
        reportStatus(processor.getResult().toString());
    }

}
