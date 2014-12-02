package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.metabroadcast.common.scheduling.UpdateProgress;

public final class BootstrapUploadTask extends UploadTask {
    
    private static final Predicate<Content> IS_ACTIVELY_PUBLISHED = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return input.isActivelyPublished();
        }
    };
    
    private final YouViewContentResolver contentResolver;
    
    public BootstrapUploadTask(YouViewService remoteClient, YouViewLastUpdatedStore lastUpdatedStore, 
            Publisher publisher, YouViewContentResolver contentResolver) {
        super(remoteClient, lastUpdatedStore, publisher);
        this.contentResolver = checkNotNull(contentResolver);
    }
    
    @Override
    public void runTask() {
        DateTime lastUpdated = new DateTime();
        Iterator<Content> allContent = contentResolver.allContent();
        
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
