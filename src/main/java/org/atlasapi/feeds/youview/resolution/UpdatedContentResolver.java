package org.atlasapi.feeds.youview.resolution;

import java.util.Iterator;

import org.atlasapi.feeds.youview.ServiceIdResolver;
import org.atlasapi.feeds.youview.ServiceIdResolverFactory;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;

import com.metabroadcast.common.time.DateTimeZones;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.joda.time.DateTimeConstants.APRIL;


public class UpdatedContentResolver implements YouViewContentResolver {

    @VisibleForTesting
    static final DateTime START_OF_TIME = new DateTime(2000, APRIL, 26, 0, 0, 0, 0, DateTimeZones.UTC);

    private static final Predicate<Content> IS_VIDEO_CONTENT = new Predicate<Content>() {
        @Override
        public boolean apply(Content input) {
            return MediaType.VIDEO.equals(input.getMediaType());
        }
    };

    private final ServiceIdResolverFactory serviceIdResolverFactory = new ServiceIdResolverFactory();
    
    private final Predicate<Content> HAS_MASTER_BRAND_MAPPING = new Predicate<Content>() {

        @Override
        public boolean apply(Content input) {
            return serviceIdResolver.resolveMasterBrandId(input).isPresent();
        }

    };
    
    private final LastUpdatedContentFinder contentFinder;
    private final Publisher publisher;
    private final ServiceIdResolver serviceIdResolver;
    private final ContentResolver contentResolver;

    public UpdatedContentResolver(
            LastUpdatedContentFinder contentFinder,
            ContentResolver contentResolver,
            Publisher publisher) {
        this.serviceIdResolver = serviceIdResolverFactory.create(publisher);
        this.contentFinder = checkNotNull(contentFinder);
        this.contentResolver = contentResolver;
        this.publisher = checkNotNull(publisher);
    }

    @Override
    public Iterator<Content> updatedSince(DateTime dateTime) {
        return filterContent(contentFinder.updatedSince(publisher, dateTime));
    }

    @Override
    public Iterator<Content> updatedBetween(DateTime from, DateTime to) {
        return filterContent(contentFinder.updatedBetween(publisher, from, to));
    }

    @Override
    public ResolvedContent findByUris(Iterable<String> uris){
        return contentResolver.findByUris(uris);
    }

    private Iterator<Content> filterContent(Iterator<Content> contentIterator) {
        if (Publisher.BBC == publisher) {
            return Iterators.filter(
                    contentIterator,
                    Predicates.and(IS_VIDEO_CONTENT, HAS_MASTER_BRAND_MAPPING)
            );
        } else {
            return Iterators.filter(
                    contentIterator,
                    IS_VIDEO_CONTENT
            );
        }
    }
}
