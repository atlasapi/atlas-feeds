package org.atlasapi.feeds.interlinking;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.persistence.content.ContentResolver;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.base.Maybe;

public class C4PlaylistToInterlinkFeedAdapter extends PlaylistToInterlinkFeedAdapter {

    private static final String ALIAS_TAG_PREFIX = "tag:pmlsc.channel4.com,2009:/programmes/";
    private static final String C4_TAG_PREFIX = "tag:www.channel4.com,2009:/programmes/";

    private static final Pattern BROADCAST_ID_PATTERN = Pattern.compile("(?:urn:)?(tag:www\\.\\w+4\\.com.*)");

    private final static Pattern CHANNEL_SPECIFIC_ID_PATTERN = Pattern.compile(
            "tag:([^,]+),(\\d{4}):slot/(C4|M4|F4|E4|4M|4S)(\\d+)");

    private static Set<String> BROADCAST_SERVICES = ImmutableSet.of(
            "http://www.channel4.com", 
            "http://www.e4.com", 
            "http://www.channel4.com/more4", 
            "http://film4.com",
            "http://www.4music.com",
            "http://www.channel4.com/4seven"
        );

    protected static final Pattern SYNTHESIZED_PATTERN = Pattern.compile("http://www.channel4.com/programmes/synthesized/[^/]+/(\\d+)");
    private final LoadingCache<ParentRef, String> parentRefToTagUriCache;

    public C4PlaylistToInterlinkFeedAdapter(final ContentResolver contentResolver) {
        parentRefToTagUriCache = CacheBuilder.newBuilder().maximumSize(200).build(new CacheLoader<ParentRef, String>() {

            @Override
            public String load(ParentRef parentRef) throws Exception {
                Maybe<Identified> firstValue = contentResolver.findByCanonicalUris(ImmutableSet.of(parentRef.getUri())).getFirstValue();
                if (!firstValue.hasValue()) {
                    throw new RuntimeException("Could not find URI " + parentRef.getUri());
                }
                return extractTagUri(firstValue.requireValue());
            }
        });

    }
    
    @Override
    protected String broadcastId(Broadcast broadcast) {
    	for (String alias : broadcast.getAliasUrls()) {
    	    Matcher matcher = CHANNEL_SPECIFIC_ID_PATTERN.matcher(alias);
			if (matcher.matches()) {
				return alias;
			}
			//check if non-channel-specific alias
			matcher = BROADCAST_ID_PATTERN.matcher(alias);
			if (matcher.matches()) {
			    String tagAlias = matcher.group(1);
			    int slashIndex = tagAlias.lastIndexOf("/")+1;
				return tagAlias.substring(0,slashIndex) + CHANNEL_LOOKUP.get(broadcast.getBroadcastOn()) + tagAlias.substring(slashIndex);
			}
		}
    	if(broadcast.getSourceId() != null) {
    	    return broadcast.getSourceId();
    	}
    	return super.broadcastId(broadcast);
    }

    @Override
    protected Predicate<Broadcast> broadcastFilter() {
        return new Predicate<Broadcast>() {

            @Override
            public boolean apply(Broadcast broadcast) {
                for (String alias : broadcast.getAliasUrls()) {
                    if (BROADCAST_ID_PATTERN.matcher(alias).matches()) {
                        return true;
                    }
                }
                return BROADCAST_SERVICES.contains(broadcast.getBroadcastOn());
            }
        };
    }

    private static final Pattern LOCATION_ID = Pattern.compile(".*(/programmes/.*/4od#\\d+)$");
    
    @Override
    protected  String idFrom(Identified description) {
        if (description instanceof Location) {
        	Location location = (Location) description;
        	Matcher idMatcher = LOCATION_ID.matcher(location.getUri());
			if (idMatcher.matches()) {
				return "tag:www.channel4.com,2009:" + idMatcher.group(1);
        	}
        }
        return extractTagUri(description);
    }
    
    @Override
    protected String idFromParentRef(ParentRef parent) {
        try {
            return parentRefToTagUriCache.get(parent);
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    private String extractTagUri(Identified identified) {
        for (String alias : identified.getAliasUrls()) {
            if (alias.startsWith(ALIAS_TAG_PREFIX)) {
                return alias.replace(ALIAS_TAG_PREFIX, C4_TAG_PREFIX);
            }
        }
        throw new IllegalArgumentException("Cannot find tag URI alias for " 
                    + identified.getCanonicalUri());
    }

    @Override
    protected String linkFrom(String canonicalUri) {
        Matcher matcher = SYNTHESIZED_PATTERN.matcher(canonicalUri);
        if (matcher.matches()) {
            return "http://www.channel4.com/tv-listings#" + matcher.group(1);
        }
        return canonicalUri;
    }
}
