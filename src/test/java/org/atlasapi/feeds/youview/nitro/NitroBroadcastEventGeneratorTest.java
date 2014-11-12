package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.services.ServiceMapping;
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


public class NitroBroadcastEventGeneratorTest {
    
    private static final String BBC_SERVICE_ID = "bbc_one_south_west";
    private static final String YOUVIEW_SERVICE_ID = "bbc_one_south_2_2871";
    private static final String BROADCAST_IMI = "broadcast_imi";
    private static final String VERSION_CRID = "version_crid";
    
    private DateTime time = new DateTime(2012, 1, 1, 0, 0, 0, 0).withZone(DateTimeZone.UTC);
    private Clock clock = new TimeMachine(time);
    private IdGenerator idGenerator = Mockito.mock(IdGenerator.class);
    private ChannelResolver channelResolver = Mockito.mock(ChannelResolver.class);
    private ServiceMapping serviceMapping = Mockito.mock(ServiceMapping.class);
    private final BroadcastEventGenerator generator;
    
    public NitroBroadcastEventGeneratorTest() throws DatatypeConfigurationException {
        this.generator = new NitroBroadcastEventGenerator(idGenerator, new TvAnytimeElementFactory(), serviceMapping, channelResolver);
    }
    
    @SuppressWarnings("deprecation") // the delights of working with classes that use Maybe
    @Before
    public void setup() {
        Channel channel = Mockito.mock(Channel.class);
        when(channelResolver.fromUri(anyString())).thenReturn(Maybe.just(channel));
        Alias alias = new Alias("bbc:service:id", BBC_SERVICE_ID);
        when(channel.getAliases()).thenReturn(ImmutableSet.of(alias));
        
        when(serviceMapping.youviewServiceIdFor(BBC_SERVICE_ID)).thenReturn(ImmutableSet.of(YOUVIEW_SERVICE_ID));
        
        when(idGenerator.generateBroadcastImi(any(Broadcast.class))).thenReturn(BROADCAST_IMI);
        when(idGenerator.generateVersionCrid(any(Item.class), any(Version.class))).thenReturn(VERSION_CRID);
    }

    @Test
    public void testGenerationOfBroadcastEventFromSingleBroadcast() {
        Broadcast broadcast = createBroadcast();
        Item item = createItemWithBroadcasts(ImmutableSet.of(broadcast));
        
        BroadcastEventType generated = Iterables.getOnlyElement(generator.generate(item));
        
        assertEquals("http://bbc.co.uk/services/" + YOUVIEW_SERVICE_ID, generated.getServiceIDRef());
        assertEquals(VERSION_CRID, generated.getProgram().getCrid());
        assertEquals("dvb://233A..A020;A876", generated.getProgramURL());
        assertEquals(BROADCAST_IMI, generated.getInstanceMetadataId());
        assertEquals("pcrid.dmol.co.uk", generated.getInstanceDescription().getOtherIdentifier().get(0).getAuthority());
        assertEquals("crid://fp.bbc.co.uk/SILG5", generated.getInstanceDescription().getOtherIdentifier().get(0).getValue());
        assertEquals("2012-01-01T00:00:00.000Z", generated.getPublishedStartTime().toString());
        // TODO is there a better way to compare generated durations?
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
