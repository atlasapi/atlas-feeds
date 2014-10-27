package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.tvanytime.TVAnytimeElementFactory;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import tva.metadata._2010.BroadcastEventType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class DefaultBroadcastEventGeneratorTest {
    
    private static final String BROADCAST_ID = "broadcast_id";
    
    private DateTime time = new DateTime(2012, 1, 1, 0, 0, 0, 0).withZone(DateTimeZone.UTC);
    private Clock clock = new TimeMachine(time);
    private BroadcastIdGenerator idGenerator = Mockito.mock(BroadcastIdGenerator.class);
    private ChannelResolver channelResolver = Mockito.mock(ChannelResolver.class);
    private final BroadcastEventGenerator generator;
    
    public DefaultBroadcastEventGeneratorTest() throws DatatypeConfigurationException {
        this.generator = new DefaultBroadcastEventGenerator(new TVAnytimeElementFactory(), idGenerator, channelResolver);
    }
    
    @Before
    public void setup() {
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(channelResolver.fromUri(Mockito.anyString())).thenReturn(Maybe.just(channel));
        Alias alias = new Alias("bbc:service:id", "bbc_one_south_west");
        Mockito.when(channel.getAliases()).thenReturn(ImmutableSet.of(alias));
        
        Mockito.when(idGenerator.generate(Mockito.any(Broadcast.class))).thenReturn(BROADCAST_ID);
    }

    @Test
    public void testGenerationOfBroadcastEventFromSingleBroadcast() {
        Broadcast broadcast = createBroadcast();
        Item item = createItemWithBroadcasts(ImmutableSet.of(broadcast));
        
        BroadcastEventType generated = Iterables.getOnlyElement(generator.generate(item));
        
        assertEquals("http://bbc.co.uk/services/bbc_one_south_west", generated.getServiceIDRef());
        assertEquals("crid://bbc.co.uk/iplayer/nitro/youview/broadcast_id", generated.getProgram().getCrid());
        assertEquals("dvb://233A..A020;A876", generated.getProgramURL());
        assertEquals("imi:www.bbc.co.uk/e290a99f07b8962a503d08f6ca1e8bb3", generated.getInstanceMetadataId());
        assertEquals("pcrid.dmol.co.uk", generated.getInstanceDescription().getOtherIdentifier().get(0).getAuthority());
        assertEquals("crid://fp.bbc.co.uk/SILG5", generated.getInstanceDescription().getOtherIdentifier().get(0).getValue());
        assertEquals("2012-01-01T00:00:00.000Z", generated.getPublishedStartTime().toString());
        assertEquals("P0DT0H30M0.000S", generated.getPublishedDuration().toString());
        assertTrue("Free should be hardcoded to true", generated.getFree().isValue());
    }

    private Item createItemWithBroadcasts(Set<Broadcast> broadcasts) {
        Item item = new Item("item", "curie", Publisher.METABROADCAST);
        Version version = new Version();
        version.setBroadcasts(broadcasts);
        item.setVersions(ImmutableSet.of(version));
        return item;
    }

    private Broadcast createBroadcast() {
        Broadcast broadcast = new Broadcast("http://www.bbc.co.uk/services/bbcone/south_west", clock.now(), clock.now().plusMinutes(30));
        broadcast.setCanonicalUri("I'm a broadcast");
        return broadcast;
    }

}
