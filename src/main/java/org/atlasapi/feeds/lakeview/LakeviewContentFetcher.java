package org.atlasapi.feeds.lakeview;

import static org.atlasapi.persistence.content.listing.ContentListingCriteria.defaultCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentTable;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.listing.ContentListingHandler;
import org.atlasapi.persistence.content.listing.ContentListingProgress;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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

    public List<LakeviewContentGroup> fetchContent() {
        
        //Container uri to episode map.
        final Multimap<String, Episode> containerAvailableEpisodes = HashMultimap.create();
        
        contentLister.listContent(ImmutableSet.of(ContentTable.CHILD_ITEMS), defaultCriteria().forPublisher(Publisher.C4), new ContentListingHandler() {
            @Override
            public boolean handle(Iterable<? extends Content> contents, ContentListingProgress progress) {
                
                for (Content content : contents) {
                    
                    if (content instanceof Episode) {
                        Episode episode = (Episode) content;
                        
                        if(hasAvailableLocation(episode)) {
                            
                            String brandUri = episode.getContainer().getUri();
                            containerAvailableEpisodes.put(brandUri, episode);
                            
                        }
                        
                    }
                    
                }
                
                return true;
            }
        });
        
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
                    if(location.getAvailable()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

}
