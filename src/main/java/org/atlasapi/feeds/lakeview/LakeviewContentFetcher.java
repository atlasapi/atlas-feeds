package org.atlasapi.feeds.lakeview;

import static org.atlasapi.media.content.util.ContentCategory.CHILD_ITEM;
import static org.atlasapi.persistence.content.listing.ContentListingCriteria.defaultCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.atlasapi.media.content.Brand;
import org.atlasapi.media.content.ChildRef;
import org.atlasapi.media.content.ContentResolver;
import org.atlasapi.media.content.Encoding;
import org.atlasapi.media.content.Episode;
import org.atlasapi.media.content.Identified;
import org.atlasapi.media.content.Location;
import org.atlasapi.media.content.Policy.Platform;
import org.atlasapi.media.content.Publisher;
import org.atlasapi.media.content.Series;
import org.atlasapi.media.content.Version;
import org.atlasapi.persistence.content.listing.ContentLister;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.base.Maybe;

public class LakeviewContentFetcher {

    private final ContentLister contentLister;
    private final ContentResolver contentResolver;
    
    private final Ordering<Series> seriesOrdering = Ordering.from(new Comparator<Series>() {
        @Override
        public int compare(Series o1, Series o2) {
            return o1.getSeriesNumber().compareTo(o2.getSeriesNumber());
        }
    });
    public static final Ordering<Episode> EPISODE_NUMBER_ORDERING = Ordering.from(new Comparator<Episode>() {
        @Override
        public int compare(Episode o1, Episode o2) {
            return o1.getEpisodeNumber().compareTo(o2.getEpisodeNumber());
        }
    });
    

    public LakeviewContentFetcher(ContentLister contentLister, ContentResolver contentResolver) {
        this.contentLister = contentLister;
        this.contentResolver = contentResolver;
    }

    public List<LakeviewContentGroup> fetchContent(Publisher publisher) {
        
        //Container uri to episode map.
        final Multimap<String, Episode> containerAvailableEpisodes = HashMultimap.create();
        
        Iterator<Episode> listContent = Iterators.filter(contentLister.listContent(defaultCriteria().forPublisher(publisher).forContent(CHILD_ITEM).build()), Episode.class);
        
        while (listContent.hasNext()) {
            Episode episode = listContent.next();
            if(hasAvailableLocation(episode)) {
                String brandUri = episode.getContainer().getUri();
                containerAvailableEpisodes.put(brandUri, episode);
            }
        }
        
        return createContentList(containerAvailableEpisodes);
    }

    private List<LakeviewContentGroup> createContentList(Multimap<String, Episode> containerAvailableEpisodes) {
        ArrayList<LakeviewContentGroup> content = Lists.newArrayList();
        
        for (Entry<String, Collection<Episode>> brandEpisodes : containerAvailableEpisodes.asMap().entrySet()) {
            
            Maybe<Identified> resolvedBrand = contentResolver.findByCanonicalUris(ImmutableList.of(brandEpisodes.getKey())).get(brandEpisodes.getKey());
            if(resolvedBrand.hasValue()) {
                
                Brand brand = (Brand)resolvedBrand.requireValue();
                List<Series> resolvedSeries = resolveSeries(Iterables.transform(brand.getSeriesRefs(), ChildRef.TO_URI));
                ImmutableList<Episode> orderedEpisodes = EPISODE_NUMBER_ORDERING.immutableSortedCopy(brandEpisodes.getValue());
                
                if (!brandEpisodes.getValue().isEmpty()) {
                    content.add(new LakeviewContentGroup(brand, resolvedSeries, orderedEpisodes));
                }
            }
        }
        
        return content;
    }

    private List<Series> resolveSeries(Iterable<String> seriesUris) {
        if(seriesUris == null || Iterables.isEmpty(seriesUris)) {
            return ImmutableList.of();
        }
        return seriesOrdering.immutableSortedCopy(Iterables.filter(contentResolver.findByCanonicalUris(seriesUris).getAllResolvedResults(), Series.class));
    }

    private boolean hasAvailableLocation(Episode episode) {
        
        for (Version version : episode.getVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                for (Location location : encoding.getAvailableAt()) {
                    if(location.getAvailable() && (location.getPolicy() != null && Platform.XBOX.equals(location.getPolicy().getPlatform()))) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

}
