package org.atlasapi.feeds.youview.resolution;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;


public class UpdatedContentResolver implements YouViewContentResolver {

    private static final Predicate<Content> IS_VIDEO_CONTENT = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return MediaType.VIDEO.equals(input.getMediaType());
        }
    };
    
    private final LastUpdatedContentFinder contentFinder;
    private final Publisher publisher;
    
    public UpdatedContentResolver(LastUpdatedContentFinder contentFinder, Publisher publisher) {
        this.contentFinder = checkNotNull(contentFinder);
        this.publisher = checkNotNull(publisher);
    }

    @Override
    public Iterator<Content> updatedSince(DateTime dateTime) {
        return fetchContentUpdatedSinceDate(dateTime);
    }
    
    private Iterator<Content> fetchContentUpdatedSinceDate(DateTime since) {
        return Iterators.filter(contentFinder.updatedSince(publisher, since), IS_VIDEO_CONTENT);
    }
}
