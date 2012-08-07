package org.atlasapi.feeds.xmltv;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import org.atlasapi.persistence.media.channel.ChannelResolver;

public class XmlTvChannelLookup {
    
	private ChannelResolver channelResolver;
	private static Pattern XMLTV_URI_MATCHER = Pattern.compile("http://xmltv.radiotimes.com/channels/(\\d+)");
	
    public XmlTvChannelLookup(ChannelResolver channelResolver) {
    	this.channelResolver = channelResolver;
    }
    
    public static class XmlTvChannel {
        
        private final Channel channel;
        private final String title;

        public XmlTvChannel(Channel channel, String title) {
            this.channel = checkNotNull(channel);
            this.title = checkNotNull(title);
        }
        
        public Channel channel() {
            return channel;
        }

        public String title() {
            return title;
        }
        
        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that instanceof XmlTvChannel) {
                XmlTvChannel other = (XmlTvChannel) that;
                return channel.equals(other.channel) && title.equals(other.title);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(channel, title);
        }

        @Override
        public String toString() {
            return String.format("XmlTv Channel: %s (%s)", channel.uri(), title);
        }
    }
    
    public Map<Integer, XmlTvChannel> getXmlTvChannelMap() {
    	
    	Builder<Integer, XmlTvChannel> map = ImmutableMap.builder();
    	for(Entry<String, Channel> entry : channelResolver.forAliases("http://xmltv.radiotimes.com/").entrySet()) {
    		
    		Matcher m = XMLTV_URI_MATCHER.matcher(entry.getKey());
    		if(m.matches()) {
    			Integer key = Integer.decode(m.group(1));
    			String overrideTitle = CHANNEL_NAME_OVERRIDES.get(key);
    			Channel channel = entry.getValue();
    			map.put(key, new XmlTvChannel(channel, overrideTitle == null ? channel.title() : overrideTitle));
    		}
    	}
    	
    	map.putAll(Maps.transformValues(DUMMY_CHANNELS, new Function<String, XmlTvChannel>() {
			@Override
			public XmlTvChannel apply(String input) {
				return new XmlTvChannel(IGNORED, input);
			} 
    	}));
    	
    	return map.build();
    }
    
    public static XmlTvChannel channelFrom(Channel channel) {
        return new XmlTvChannel(channel, channel.title());
    }
    
    public static XmlTvChannel channelFrom(Channel channel, String title) {
        return new XmlTvChannel(channel, title);
    }

    public static final Channel IGNORED = new Channel(Publisher.METABROADCAST, "Ignored", "Ignored", MediaType.AUDIO, "ignored");
    
    private static final ImmutableMap<Integer, String> CHANNEL_NAME_OVERRIDES = ImmutableMap.<Integer, String> builder()
    		.put(32, "YTV")
    		.put(45, "BBC3")
    		.put(47, "BBC4")
    		.put(92, "BBC1")
    		.put(93, "BBC1 East")
    		.put(94, "BBC1 London")
    		.put(95, "BBC1 Midlands")
    		.put(96, "BBC1 North")
    		.put(97, "BBC1 North East")
    		.put(98, "BBC1 North West")
    		.put(99, "BBC1 Northern Ireland")
    		.put(100, "BBC1 Wales")
    		.put(101, "BBC1 Scotland")
    		.put(102, "BBC1 South")
    		.put(103, "BBC1 South West")
    		.put(104, "BBC1 West")
    		.put(105, "BBC2")
            .put(106, "BBC2 East")
            .put(107, "BBC2 London")
            .put(108, "BBC2 Midlands")
            .put(109, "BBC2 North West")
            .put(110, "BBC2 North")
            .put(111, "BBC2 North East")
            .put(112, "BBC2 Northern Ireland")
            .put(113, "BBC2 Scotland")
            .put(114, "BBC2 Wales")
            .put(115, "BBC2 South")
            .put(116, "BBC2 South West")
            .put(117, "BBC2 West")
            .put(119, "Bio")
            .put(123, "Eurosport")
            .put(134, "Channel 5")
            .put(137, "GOD Channel")
            .put(147, "Discovery Channel")
            .put(149, "Discovery Home & Health")
            .put(152, "Discovery Channel +1")
            .put(154, "Discovery Travel & Living")
            .put(159, "EuroNews")
            .put(160, "Film4")
            .put(165, "Disney XD")
            .put(166, "Disney XD +1")
            .put(191, "Kerrang!")
            .put(231, "RTE1")
            .put(248, "Sky1")
            .put(266, "Sky Real Lives")
            .put(273, "TG4")
            .put(421, "Disney Junior")//AKA Disney Junior
            .put(588, "Magic")
            .put(592, "Smash Hits!")
            .put(609, "Kiss")
            .put(661, "Attheraces")
            .put(801, "Yesterday")
            .put(922, "Sky2")
            .put(941, "TV3 (Spanish)")
            .put(1143, "Scuzz")
            .put(1144, "Bliss")
            .put(1261, "Sky Movies Box Office")//AKA Sky Movies Box Office
            .put(1421, "E! Entertainment")//AKA E! Entertainment
            .put(1462, "travelchannel")
            .put(1543, "ESPN America")
            .put(1544, "4Music")
            .put(1601, "Eden")
            .put(1602, "Blighty")
            .put(1662, "Sky Real Lives 2")
            .put(1802, "Ideal World")
            .put(1804, "TeleG")
            .put(1855, "travelchannel +1")
            .put(1862, "BBC2 South East")
            .put(1869, "BBC1 South East")
            .put(1870, "RTE2")
            .put(1882, "Really")
            .put(1944, "Channel M")
            .put(1949, "Racing UK")
            .put(1953, "Discovery Home & Health +1")
            .put(1956, "Teachers TV")
            .put(1959, "More4")
            .put(1969, "Fashion TV")
            .put(1981, "CITV")
            .put(1983, "Cartoonito")
            .put(2008, "5USA")
            .put(2011, "ESPN Classic")
            .put(2021, "Film4 +1")
            .put(2040, "BBC Sport Interactive: (Freeview)")
            .put(2050, "Dave")
            .put(2052, "Dave ja vu")
            .put(2058, "Movies4Men")
            .put(2059, "True Movies 1")
            .put(2062, "5*")
            .put(2118, "ITV1 London HD")
            .put(2134, "Home")
            .put(2139, "Sky1 HD")
            .put(2144, "Crime & Investigation HD")
            .put(2149, "FX HD")
            .put(2150, "MTVN HD")
            .put(2161, "Sky Movies Sci-Fi/Horror HD")
            .put(2162, "Sky Real Lives HD")
            .put(2165, "Crime & Investigation")
            .put(2168, "Sky Movies Sci-Fi/Horror")
            .put(2179, "Quest")
            .put(2181, "Cinemoi")
            .put(2184, "Viva")
            .put(2197, "Christmas 24")
            .put(2203, "STV North")
            .put(2204, "Sky Movies Action & Adventure")
            .put(2205, "Sky Movies Action & Adventure HD")
            .put(2206, "Sky Movies Crime & Thriller")
            .put(2207, "Sky Movies Crime & Thriller HD")
            .put(2208, "Sky Movies Drama & Romance")
            .put(2209, "Sky Movies Drama & Romance HD")
            .put(2212, "Syfy")
            .put(2214, "Syfy HD")
            .put(2219, "STV HD")
            .put(2240, "BBC1 East Midlands")
            .put(2241, "BBC2 East Midlands")
            .put(2246, "Phoenix CNE")
            .put(2249, "Rai Uno")
            .put(2250, "Health")
            .put(2252, "Music Choice Blues")
            .put(2253, "Music Choice Classical")
            .put(2254, "Music Choice Country")
            .put(2255, "Music Choice Dance")
            .put(2256, "Music Choice Easy Listening")
            .put(2257, "Music Choice Gold")
            .put(2258, "Music Choice Hits")
            .put(2259, "Music Choice Jazz")
            .put(2260, "Music Choice Love")
            .put(2261, "Music Choice Rock")
            .put(2264, "Sat 1")
            .put(2421, "Asia 1")
            .put(2422, "The Pakistani Channel")
            .put(2457, "Flaunt")
            .put(2492, "City Channel")
            .put(2502, "3e")
            .put(2505, "FilmFlex")
            .put(2506, "Sky Movies Box Office HD2")
            .put(2507, "Sky Movies Box Office HD1")
            .put(2518, "E! Europe")
            .put(2531, "Channel S")
            .put(2542, "Playhouse Disney+")
            .put(2544, "Pop")
            .put(2549, "Horse & Country")
            .put(2553, "ABC News Now")
            .put(2558, "Movies4Men2")
            .put(2572, "Rocks & Co")
            .put(2574, "Discovery Travel & Living +1")
            .put(2575, "Sumo TV")
            .put(2577, "5USA +1")
            .put(2578, "5* +1")
            .put(2583, "Crime & Investigation +1")
            .put(2585, "Sky Living HD") //AKA Sky Living HD
            .put(2590, "Christmas 24+")
            .put(2598, "Movies4Men +1")
            .put(2599, "Movies4Men2 +1")
            .put(2601, "Discovery Knowledge +1")
            .put(2603, "TrueEnt")
            .put(2604, "Body in Balance")
            .put(2610, "BBC Sport Interactive: BBC2")
            .put(2612, "BBC Sport Interactive: BBC1")
            .put(2613, "BBC Sport Interactive: BBC3")
            .put(2615, "Channel 5 HD")
            .put(2617, "Film4 HD")
            .put(2618, "Blighty +1")
            .put(2619, "Discovery +1.5")
            .put(2622, "Showcase")
            .put(2625, "Asianet")
            .put(2636, "Multi Channel")
            .put(2637, "Music Choice Europe")
            .put(2640, "Price-drop.tv")
            .put(2642, "TV3")
            .put(2645, "MGM HD")
            .put(2663, "Quest (Freeview)")
            .put(2667, "BBC1 HD")
            .put(2681, "ITV1 STV +1")
            .put(2696, "Sony Entertainment TV (Plus 1)")
            .put(2697, "Sony Entertainment Television")
            .build();
    
    private static final ImmutableMap<Integer, String> DUMMY_CHANNELS = ImmutableMap.<Integer, String> builder()
            .put(246, "Screenshop")
            .put(266, "Sky Real Lives")
            .put(1662, "Sky Real Lives 2")
            .put(1802, "Ideal World")
            .put(1944, "Channel M")
            .put(1956, "Teachers TV")
            .put(1969, "Fashion TV")
            .put(2162, "Sky Real Lives HD")
            .put(2249, "Rai Uno")
            .put(2250, "Health")
            .put(2252, "Music Choice Blues")
            .put(2253, "Music Choice Classical")
            .put(2254, "Music Choice Country")
            .put(2255, "Music Choice Dance")
            .put(2256, "Music Choice Easy Listening")
            .put(2257, "Music Choice Gold")
            .put(2258, "Music Choice Hits")
            .put(2259, "Music Choice Jazz")
            .put(2260, "Music Choice Love")
            .put(2261, "Music Choice Rock")
            .put(2264, "Sat 1")
            .put(2421, "Asia 1")
            .put(2422, "The Pakistani Channel")
            .put(2492, "City Channel")
            .put(2506, "Sky Movies Box Office HD2")
            .put(2507, "Sky Movies Box Office HD1")
            .put(2531, "Channel S")
            .put(2553, "ABC News Now")
            .put(2572, "Rocks & Co")
            .put(2575, "Sumo TV")
            .put(2601, "Discovery Knowledge +1")
            .put(2618, "Blighty +1")
            .put(2622, "Showcase")
            .put(2625, "Asianet")
            .put(2636, "Multi Channel")
            .put(2637, "Music Choice Europe")
            .put(2681, "ITV1 STV +1")
            .put(2696, "Sony Entertainment TV (Plus 1)")
            .put(2697, "Sony Entertainment Television")
    		.build();


}
