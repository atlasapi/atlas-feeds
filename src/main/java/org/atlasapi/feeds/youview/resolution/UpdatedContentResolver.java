package org.atlasapi.feeds.youview.resolution;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.joda.time.DateTimeConstants.APRIL;

import java.util.Iterator;

import org.atlasapi.feeds.youview.ServiceIdResolver;
import org.atlasapi.feeds.youview.ServiceIdResolverFactory;
import org.atlasapi.feeds.youview.nitro.NitroServiceIdResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.metabroadcast.common.time.DateTimeZones;


public class UpdatedContentResolver implements YouViewContentResolver {

    @VisibleForTesting
    // TODO update this to point back to the genuine start of time once testing is complete
    static final DateTime START_OF_TIME = new DateTime(2015, APRIL, 26, 0, 0, 0, 0, DateTimeZones.UTC);

    private static final Predicate<Content> IS_VIDEO_CONTENT = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return MediaType.VIDEO.equals(input.getMediaType());
        }
    };
    
    private final Predicate<Content> HAS_MASTER_BRAND_MAPPING = new Predicate<Content>() {

        @Override
        public boolean apply(Content input) {
            return serviceIdResolver.resolveMasterBrandId(input).isPresent();
        }
        
    };
    
    private final LastUpdatedContentFinder contentFinder;
    private final Publisher publisher;
    private final ServiceIdResolver serviceIdResolver;
    
    public UpdatedContentResolver(
            LastUpdatedContentFinder contentFinder,
            Publisher publisher) {
        ServiceIdResolverFactory serviceIdResolverFactory = new ServiceIdResolverFactory();
        this.serviceIdResolver = serviceIdResolverFactory.create(publisher);
        this.contentFinder = checkNotNull(contentFinder);
        this.publisher = checkNotNull(publisher);
    }

    @Override
    public Iterator<Content> updatedSince(DateTime dateTime) {
        return fetchContentUpdatedSinceDate(dateTime);
    }

    private Iterator<Content> fetchContentUpdatedSinceDate(DateTime since) {
        //bbc specific hack. I don't know whence this requirement stems.
        if (Publisher.BBC == publisher) {
            return Iterators.filter(
                    contentFinder.updatedSince(publisher, since),
                    Predicates.and(IS_VIDEO_CONTENT, HAS_MASTER_BRAND_MAPPING)
            );
        } else {
            return Iterators.filter(
                    contentFinder.updatedSince(publisher, since),
                    IS_VIDEO_CONTENT
            );
        }
    }
}
