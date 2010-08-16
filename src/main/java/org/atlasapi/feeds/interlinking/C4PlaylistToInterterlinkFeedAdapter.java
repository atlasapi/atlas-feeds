package org.atlasapi.feeds.interlinking;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Description;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

public class C4PlaylistToInterterlinkFeedAdapter extends PlaylistToInterlinkFeedAdapter {
    
    private static final String BROADCAST_ID_PREFIX = "tag:";
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
    protected InterlinkBroadcast fromBroadcast(Broadcast broadcast, InterlinkEpisode episode) {
        String id = null;
        for (String alias : broadcast.getAliases()) {
            if (alias.startsWith(BROADCAST_ID_PREFIX) || alias.startsWith("urn:"+BROADCAST_ID_PREFIX)) {
                id = alias;
                break;
            }
        }

        if (id != null) {
            return new InterlinkBroadcast(id, DEFAULT_OPERATION, episode).withDuration(toDuration(broadcast.getBroadcastDuration())).withBroadcastStart(broadcast.getTransmissionTime()).withLastUpdated(broadcast.getLastUpdated());
        }
        return null;
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
