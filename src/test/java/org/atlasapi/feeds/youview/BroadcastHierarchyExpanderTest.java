package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.services.BroadcastServiceMapping;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class BroadcastHierarchyExpanderTest {

    private Clock clock = new TimeMachine();
    private IdGenerator idGenerator;
    private BroadcastServiceMapping serviceMapping = Mockito.mock(BroadcastServiceMapping.class);
    private BbcServiceIdResolver serviceIdResolver = Mockito.mock(BbcServiceIdResolver.class);
    private BroadcastHierarchyExpander hierarchyExpander;
    
    @Test
    public void testCombinesEqualIdsInMapping() {
        idGenerator = Mockito.mock(IdGenerator.class);
        hierarchyExpander = new BroadcastHierarchyExpander(idGenerator, serviceMapping, serviceIdResolver);
        
        when(idGenerator.generateBroadcastImi(anyString(), any(Broadcast.class))).thenReturn("broadcast_imi");
        when(serviceMapping.youviewServiceIdFor(anyString())).thenReturn(ImmutableSet.of("youviewServiceId"));
        
        Set<Version> versions = createNVersions(3);
        Set<Broadcast> broadcasts = createNBroadcasts(2);
        Item item = createItem(versions, broadcasts);
        Map<String, ItemBroadcastHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(1, hierarchy.size());
    }
    
    @Test
    public void testIfBroadcastImisUniqueExpandsAllVersions() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new BroadcastHierarchyExpander(idGenerator, serviceMapping, serviceIdResolver);
        
        when(serviceMapping.youviewServiceIdFor(anyString())).thenReturn(ImmutableSet.of("youviewServiceId"));
        
        int numVersions = 3;
        
        Set<Version> versions = createNVersions(numVersions);
        Set<Broadcast> broadcasts = createNBroadcasts(1);
        Item item = createItem(versions, broadcasts);
        Map<String, ItemBroadcastHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(numVersions, hierarchy.size());
    }
    
    @Test
    public void testIfBroadcastImisUniqueExpandsAllBroadcasts() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new BroadcastHierarchyExpander(idGenerator, serviceMapping, serviceIdResolver);
        
        when(serviceMapping.youviewServiceIdFor(anyString())).thenReturn(ImmutableSet.of("youviewServiceId"));
        
        int numBroadcasts = 3;
        
        Set<Version> versions = createNVersions(1);
        Set<Broadcast> broadcasts = createNBroadcasts(numBroadcasts);
        Item item = createItem(versions, broadcasts);
        Map<String, ItemBroadcastHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(numBroadcasts, hierarchy.size());
    }
    
    @Test
    public void testIfBroadcastImisUniqueExpandsAllYouViewServiceIds() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new BroadcastHierarchyExpander(idGenerator, serviceMapping, serviceIdResolver);
        
        ImmutableSet<String> youViewServiceIds = ImmutableSet.of("yvSID_1", "yvSID_2");
        when(serviceMapping.youviewServiceIdFor(anyString())).thenReturn(youViewServiceIds);
        
        
        Set<Version> versions = createNVersions(1);
        Set<Broadcast> broadcasts = createNBroadcasts(1);
        Item item = createItem(versions, broadcasts);
        Map<String, ItemBroadcastHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(youViewServiceIds.size(), hierarchy.size());
    }

    private Item createItem(Set<Version> versions, Set<Broadcast> broadcasts) {
        Item item = new Item("uri", "curie", Publisher.METABROADCAST);
        for (Version version : versions) {
            version.setBroadcasts(broadcasts);
        }
        item.setVersions(versions);
        return item;
    }

    private Set<Version> createNVersions(int numVersions) {
        ImmutableSet.Builder<Version> versions = ImmutableSet.builder();
        for (int i = 0; i < numVersions; i++) {
            Version version = new Version();
            version.setCanonicalUri("version_" + i);
            versions.add(version);
        }
        return versions.build();
    }

    private Set<Broadcast> createNBroadcasts(int numBroadcasts) {
        ImmutableSet.Builder<Broadcast> broadcasts = ImmutableSet.builder();
        for (int i = 0; i < numBroadcasts; i++) {
            broadcasts.add(createBroadcast("broadcast_" + i));
        }
        return broadcasts.build();
    }

    private Broadcast createBroadcast(String sourceId) {
        Broadcast broadcast = new Broadcast("http://bbc.co.uk/services/bbcone", clock.now(), clock.now().plusMinutes(30));
        broadcast.withId(sourceId);
        return broadcast;
    }

}
