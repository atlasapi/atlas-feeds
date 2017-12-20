package org.atlasapi.feeds.youview.resolution;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ResolvedContent;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;


public class FullHierarchyResolvingContentResolver implements YouViewContentResolver {
    
    private final Logger log = LoggerFactory.getLogger(FullHierarchyResolvingContentResolver.class);
    
    private final YouViewContentResolver delegate;
    private final ContentHierarchyExtractor hierarchyExtractor;
    
    public FullHierarchyResolvingContentResolver(YouViewContentResolver delegate,
            ContentHierarchyExtractor hierarchyExtractor) {
        this.delegate = checkNotNull(delegate);
        this.hierarchyExtractor = checkNotNull(hierarchyExtractor);
    }

    @Override
    public Iterator<Content> updatedSince(DateTime dateTime) {
        return resolveHierarchyFor(delegate.updatedSince(dateTime));
    }

    @Override
    public ResolvedContent findByUris(Iterable<String> uris) {
        return delegate.findByUris(uris);
    }

    private Iterator<Content> resolveHierarchyFor(Iterator<Content> content) {
        return Iterators.concat(Iterators.transform(content, new Function<Content, Iterator<Content>>() {
            @Override
            public Iterator<Content> apply(Content input) {
                return generateHierarchyFor(input);
            }
        }));
    }

    private Iterator<Content> generateHierarchyFor(Content input) {
        ImmutableSet.Builder<Content> hierarchy = ImmutableSet.builder();
        log.trace("expanding hierarchy for {}", input.getCanonicalUri());
        hierarchy.add(input);
        
        if (input instanceof Item) {
            Item item = (Item) input;
            Optional<Brand> brand = hierarchyExtractor.brandFor(item);
            if (brand.isPresent()) {
                hierarchy.add(brand.get());
            }
            Optional<Series> series = hierarchyExtractor.seriesFor(item);
            if (series.isPresent()) {
                hierarchy.add(series.get());
            }
        }
        
        if (input instanceof Series) {
            Optional<Brand> brand = hierarchyExtractor.brandFor((Series) input);
            if (brand.isPresent()) {
                hierarchy.add(brand.get());
            }
        }
        
        return hierarchy.build().iterator();
    }

}
