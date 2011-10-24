package org.atlasapi.feeds.lakeview;

import static org.atlasapi.persistence.content.ContentCategory.CHILD_ITEM;
import static org.atlasapi.persistence.content.listing.ContentListingCriteria.defaultCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.listing.ContentLister;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import com.metabroadcast.common.base.Maybe;

public class LakeviewContentFetcher {

    private final ContentLister contentLister;
    private final ContentResolver contentResolver;

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
            
            Maybe<Identified> brand = contentResolver.findByCanonicalUris(ImmutableList.of(brandEpisodes.getKey())).get(brandEpisodes.getKey());
            if(brand.hasValue()) {
                
                Multimap<String, Episode> seriesEpisodes = HashMultimap.create();
                for (Episode episode : brandEpisodes.getValue()) {
                    ParentRef seriesRef = episode.getSeriesRef();
                    if(seriesRef != null) {
                        seriesEpisodes.put(seriesRef.getUri(), episode);
                    }
                }
                
                List<Series> resolvedSeries = resolveSeries(seriesEpisodes.keySet());

                SetMultimap<Series, Episode> contents = TreeMultimap.create(new Comparator<Series>() {
                    @Override
                    public int compare(Series o1, Series o2) {
                        return o1.getSeriesNumber().compareTo(o2.getSeriesNumber());
                    }
                }, new Comparator<Episode>() {
                    @Override
                    public int compare(Episode o1, Episode o2) {
                        return o1.getEpisodeNumber().compareTo(o2.getEpisodeNumber());
                    }
                });
                
                for (Series series : resolvedSeries) {
                    if(series.getParent() != null) {
                        contents.putAll(series, seriesEpisodes.get(series.getCanonicalUri()));
                    }
                }
                
                if(!contents.isEmpty()) {
                    content.add(new LakeviewContentGroup((Brand)brand.requireValue(), contents.asMap()));
                }
            }
        }
        
        return content;
    }

    private List<Series> resolveSeries(Iterable<String> seriesUris) {
        if(seriesUris == null || Iterables.isEmpty(seriesUris)) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(Iterables.filter(contentResolver.findByCanonicalUris(seriesUris).getAllResolvedResults(), Series.class));
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
