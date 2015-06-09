package org.atlasapi.feeds.youview.resolution;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;


public class UpdatedContentResolver implements YouViewContentResolver {

    private static final Predicate<Content> IS_VIDEO_CONTENT = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return MediaType.VIDEO.equals(input.getMediaType());
        }
    };
    
    private final Predicate<Content> HAS_MASTER_BRAND_MAPPING = new Predicate<Content>() {

        @Override
        public boolean apply(Content input) {
            return bbcServiceIdResolver.resolveMasterBrandId(input).isPresent();
        }
        
    };
    
    private final LastUpdatedContentFinder contentFinder;
    private final Publisher publisher;
    private final BbcServiceIdResolver bbcServiceIdResolver;
    
    public UpdatedContentResolver(LastUpdatedContentFinder contentFinder, BbcServiceIdResolver bbcServiceIdResolver, 
            Publisher publisher) {
        this.contentFinder = checkNotNull(contentFinder);
        this.publisher = checkNotNull(publisher);
        this.bbcServiceIdResolver = checkNotNull(bbcServiceIdResolver);
    }

    @Override
    public Iterator<Content> updatedSince(DateTime dateTime) {
        return fetchContentUpdatedSinceDate(dateTime);
    }
    
    private Iterator<Content> fetchContentUpdatedSinceDate(DateTime since) {
        return Iterators.filter(contentFinder.updatedSince(publisher, since), 
                                Predicates.and(IS_VIDEO_CONTENT, HAS_MASTER_BRAND_MAPPING));
    }
}
