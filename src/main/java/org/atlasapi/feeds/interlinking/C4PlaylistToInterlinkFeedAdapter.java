package org.atlasapi.feeds.interlinking;

import static org.atlasapi.media.entity.Channel.CHANNEL_FOUR;
import static org.atlasapi.media.entity.Channel.E_FOUR;
import static org.atlasapi.media.entity.Channel.FILM_4;
import static org.atlasapi.media.entity.Channel.FOUR_MUSIC;
import static org.atlasapi.media.entity.Channel.MORE_FOUR;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;

public class C4PlaylistToInterlinkFeedAdapter extends PlaylistToInterlinkFeedAdapter {
    
    private static final Pattern BROADCAST_ID_PATTERN = Pattern.compile("(?:urn:)?(tag:www\\.channel4\\.com.*)");
    private static Set<String> BROADCAST_SERVICES = ImmutableSet.of(CHANNEL_FOUR.uri(), E_FOUR.uri(), MORE_FOUR.uri(), FOUR_MUSIC.uri(), FILM_4.uri());
    
    private final ConcurrentMap<String, Series> seriesLookup;

    protected static final Pattern SYNTHESIZED_PATTERN = Pattern.compile("http://www.channel4.com/programmes/synthesized/[^/]+/(\\d+)");
    
    public C4PlaylistToInterlinkFeedAdapter(final ContentResolver resolver) {
    	seriesLookup = new MapMaker().expireAfterAccess(10, TimeUnit.MINUTES).makeComputingMap(new Function<String, Series>() {
			@Override
			public Series apply(String uri) {
				return (Series) resolver.findByCanonicalUri(uri);
			}
    	});
    }
    
    @Override
    protected String broadcastId(Broadcast broadcast) {
    	for (String alias : broadcast.getAliases()) {
			Matcher matcher = BROADCAST_ID_PATTERN.matcher(alias);
			if (matcher.matches()) {
				return matcher.group(1);
			}
		}
    	if(broadcast.getId() != null) {
    	    return broadcast.getId();
    	}
    	return super.broadcastId(broadcast);
    }
    
    @Override
    protected Predicate<Broadcast> broadcastFilter() {
    	return new Predicate<Broadcast>() {

			@Override
			public boolean apply(Broadcast broadcast) {
				for (String alias : broadcast.getAliases()) {
		            if (BROADCAST_ID_PATTERN.matcher(alias).matches()) {
		            	return true;
		            }
		        }
				return BROADCAST_SERVICES.contains(broadcast.getBroadcastOn());
			}
		};
    }
    
    @Override
    protected String idFrom(Identified description) {
    	// Lookup full series if it is an embedded series
    	if (description instanceof Series) {
    	    description = seriesLookup.get(description.getCanonicalUri());
    	}
    	for (String alias : description.getAliases()) {
			if (alias.startsWith("tag:www.channel4.com") || alias.startsWith("urn:tag:www.channel4.com")) {
				return alias;
			}
		}
    	return description.getCanonicalUri();
    }
    
    @Override
    protected String linkFrom(String canonicalUri) {
        Matcher matcher = SYNTHESIZED_PATTERN.matcher(canonicalUri);
        if(matcher.matches()) {
            return "http://www.channel4.com/tv-listings#" + matcher.group(1);
        }
        return canonicalUri;
    }
}
