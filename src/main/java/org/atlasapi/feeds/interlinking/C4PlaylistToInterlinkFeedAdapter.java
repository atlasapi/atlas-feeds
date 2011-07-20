package org.atlasapi.feeds.interlinking;

import static org.atlasapi.media.entity.Channel.CHANNEL_FOUR;
import static org.atlasapi.media.entity.Channel.E_FOUR;
import static org.atlasapi.media.entity.Channel.FILM_4;
import static org.atlasapi.media.entity.Channel.MORE_FOUR;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

public class C4PlaylistToInterlinkFeedAdapter extends PlaylistToInterlinkFeedAdapter {

    private static final String C4_SLASH_PROGRAMMES_PREFIX = "http://www.channel4.com/programmes/";
    private static final String C4_TAG_PREFIX = "tag:www.channel4.com,2009:/programmes/";

    private static final Pattern BROADCAST_ID_PATTERN = Pattern.compile("(?:urn:)?(tag:www\\.\\w+4\\.com.*)");

    private final static Pattern CHANNEL_SPECIFIC_ID_PATTERN = Pattern.compile("tag:([^,]+),(\\d{4}):slot/(C4|M4|F4|E4|4M)(\\d+)");

    private static Set<String> BROADCAST_SERVICES = ImmutableSet.of(CHANNEL_FOUR.uri(), E_FOUR.uri(), MORE_FOUR.uri(), FILM_4.uri());

    protected static final Pattern SYNTHESIZED_PATTERN = Pattern.compile("http://www.channel4.com/programmes/synthesized/[^/]+/(\\d+)");


    @Override
    protected String broadcastId(Broadcast broadcast) {
    	for (String alias : broadcast.getAliases()) {
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

    private static final Pattern LOCATION_ID = Pattern.compile(".*(/programmes/.*/4od#\\d+)$");
    
    @Override
    protected  String idFrom(Identified description) {
        for (String alias : description.getAliases()) {
            if (alias.startsWith("tag:www.channel4.com") || alias.startsWith("urn:tag:www.channel4.com") || alias.startsWith("tag:www.e4.com")) {
                return alias;
            }
        }
        if (description instanceof Location) {
        	Location location = (Location) description;
        	Matcher idMatcher = LOCATION_ID.matcher(location.getUri());
			if (idMatcher.matches()) {
				return "tag:www.channel4.com,2009:" + idMatcher.group(1);
        	}
        }
        return description.getCanonicalUri();
    }
    
    @Override
    protected String idFromParentRef(ParentRef parent) {
        String parentUri = parent.getUri();
        if (!parentUri.startsWith(C4_SLASH_PROGRAMMES_PREFIX)) {
            throw new IllegalArgumentException("Parent " + parentUri + " has an invalid C4 canonical uri");
        }
        return parentUri.replace(C4_SLASH_PROGRAMMES_PREFIX, C4_TAG_PREFIX);
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
