package org.atlasapi.feeds.youview.upload.granular;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.metabroadcast.common.scheduling.UpdateProgress;

public final class GranularBootstrapUploadTask extends GranularUploadTask {
    
    private static final Predicate<Content> IS_ACTIVELY_PUBLISHED = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return input.isActivelyPublished();
        }
    };
    private final YouViewContentResolver contentResolver;
    
    public GranularBootstrapUploadTask(GranularYouViewService youViewService, YouViewLastUpdatedStore lastUpdatedStore, 
            Publisher publisher, YouViewContentResolver contentResolver, 
            ContentHierarchyExpander hierarchyExpander, IdGenerator idGenerator) {
        super(youViewService, lastUpdatedStore, publisher, hierarchyExpander, idGenerator);
        this.contentResolver = checkNotNull(contentResolver);
    }
    
    @Override
    public void runTask() {
        DateTime lastUpdated = new DateTime();
        Iterator<Content> allContent = contentResolver.allContent();
        
        GranularYouViewContentProcessor<UpdateProgress> processor = uploadProcessor(Optional.<DateTime>absent());
        
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
