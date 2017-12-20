package org.atlasapi.feeds.youview.nitro;

import javax.xml.datatype.DatatypeConfigurationException;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.services.BroadcastServiceMapping;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import tva.metadata._2010.BroadcastEventType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;


public class NitroBroadcastEventGeneratorTest {
    
    private static final String BBC_SERVICE_ID = "bbc_one_south_west";
    private static final String YOUVIEW_SERVICE_ID = "bbc_one_south_2_2871";
    private static final String BROADCAST_IMI = "broadcast_imi";
    private static final String VERSION_CRID = "version_crid";
    private static final String TERRESTRIAL_EVENT_LOCATOR = "dvb://233A..4600;938C";
    private static final String TERRESTRIAL_PROGRAMME_CRID = "crid://fp.bbc.co.uk/241B1S";

    private DateTime time = new DateTime(2012, 1, 1, 0, 0, 0, 0).withZone(DateTimeZone.UTC);
    private Clock clock = new TimeMachine(time);
    private IdGenerator idGenerator = Mockito.mock(IdGenerator.class);
    private BroadcastServiceMapping serviceMapping = Mockito.mock(BroadcastServiceMapping.class);
    private NitroServiceIdResolver bbcServiceIdResolver = Mockito.mock(NitroServiceIdResolver.class);
    
    private final BroadcastEventGenerator generator;
    
    public NitroBroadcastEventGeneratorTest() throws DatatypeConfigurationException {
        this.generator = new NitroBroadcastEventGenerator(idGenerator);
    }
    
    @Before
    public void setup() {
        Channel channel = Mockito.mock(Channel.class);
        when(bbcServiceIdResolver.resolveSId(any(Broadcast.class))).thenReturn(Optional.of(BBC_SERVICE_ID));
        Alias alias = new Alias("bbc:service:id", BBC_SERVICE_ID);
        when(channel.getAliases()).thenReturn(ImmutableSet.of(alias));
        
        when(idGenerator.generateBroadcastImi(eq(YOUVIEW_SERVICE_ID), any(Broadcast.class))).thenReturn(BROADCAST_IMI);
        when(idGenerator.generateBroadcastImi(eq(YOUVIEW_SERVICE_ID + "_1"), any(Broadcast.class))).thenReturn(BROADCAST_IMI + "_1");
        when(idGenerator.generateBroadcastImi(eq(YOUVIEW_SERVICE_ID + "_2"), any(Broadcast.class))).thenReturn(BROADCAST_IMI + "_2");
        when(idGenerator.generateVersionCrid(any(Item.class), any(Version.class))).thenReturn(VERSION_CRID);
    }

    @Test
    public void testGenerationOfBroadcastEventFromSingleBroadcast() {
        
        when(serviceMapping.youviewServiceIdFor(BBC_SERVICE_ID)).thenReturn(ImmutableSet.of(YOUVIEW_SERVICE_ID));
        
        Broadcast broadcast = createBroadcast();
        ItemBroadcastHierarchy broadcastHierarchy = createItemHierarchyFrom(broadcast);
        
        BroadcastEventType generated = generator.generate(broadcastHierarchy, BROADCAST_IMI);
        
        assertEquals("http://nitro.bbc.co.uk/services/" + YOUVIEW_SERVICE_ID, generated.getServiceIDRef());
        assertEquals(VERSION_CRID, generated.getProgram().getCrid());
        assertEquals(TERRESTRIAL_EVENT_LOCATOR, generated.getProgramURL());
        assertEquals(BROADCAST_IMI, generated.getInstanceMetadataId());
        assertEquals("pcrid.dmol.co.uk", generated.getInstanceDescription().getOtherIdentifier().get(0).getAuthority());
        assertEquals(TERRESTRIAL_PROGRAMME_CRID, generated.getInstanceDescription().getOtherIdentifier().get(0).getValue());
        assertEquals("2012-01-01T00:00:00Z", generated.getPublishedStartTime().toString());
        assertEquals("p123456", generated.getInstanceDescription().getOtherIdentifier().get(1).getValue());
        assertEquals("bpid.bbc.co.uk", generated.getInstanceDescription().getOtherIdentifier().get(1).getAuthority());
        // TODO is there a better way to compare generated durations?
        assertEquals("P0DT0H30M0.000S", generated.getPublishedDuration().toString());
        assertTrue("Free should be hardcoded to true", generated.getFree().isValue());
    }

    private ItemBroadcastHierarchy createItemHierarchyFrom(Broadcast broadcast) {
        
        Item item = new Item("item", "curie", Publisher.METABROADCAST);
        Version version = new Version();
        version.setBroadcasts(ImmutableSet.of(broadcast));
        item.setVersions(ImmutableSet.of(version));
        
        return new ItemBroadcastHierarchy(item, version, broadcast, YOUVIEW_SERVICE_ID);
    }

    private Broadcast createBroadcast() {
        Broadcast broadcast = new Broadcast("http://www.bbc.co.uk/services/bbcone/south_west", clock.now(), clock.now().plusMinutes(30));
        broadcast.setCanonicalUri("I'm a broadcast");
        broadcast.withId("bbc:p123456");
        broadcast.setAliases(aliases());
        return broadcast;
    }

    private ImmutableList<Alias> aliases() {
        return ImmutableList.of(
                new Alias("bbc:terrestrial_event_locator:teleview", TERRESTRIAL_EVENT_LOCATOR),
                new Alias("bbc:terrestrial_programme_crid:teleview", TERRESTRIAL_PROGRAMME_CRID)
        );
    }

}
