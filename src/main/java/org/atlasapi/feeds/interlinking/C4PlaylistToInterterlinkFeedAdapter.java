package org.atlasapi.feeds.interlinking;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Description;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.MapMaker;

public class C4PlaylistToInterterlinkFeedAdapter extends PlaylistToInterlinkFeedAdapter {
    
    private static final Pattern BROADCAST_ID_PATTERN = Pattern.compile("(?:urn:)?(tag:www\\.channel4\\.com.*)");
    private final ConcurrentMap<String, Series> seriesLookup;
    
    public C4PlaylistToInterterlinkFeedAdapter(final ContentResolver resolver) {
    	seriesLookup = new MapMaker().expiration(10, TimeUnit.MINUTES).makeComputingMap(new Function<String, Series>() {
			@Override
			public Series apply(String uri) {
				return (Series) resolver.findByUri(uri);
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
    	throw new IllegalStateException("Could not find id for broadcast " + broadcast);
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
				return false;
			}
		};
    }
    
    @Override
    protected String idFrom(Description description) {
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
}
