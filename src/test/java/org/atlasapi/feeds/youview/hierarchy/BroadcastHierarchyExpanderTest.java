package org.atlasapi.feeds.youview.hierarchy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.atlasapi.feeds.youview.ServiceIdResolver;
import org.atlasapi.feeds.youview.UniqueIdGenerator;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroServiceIdResolver;
import org.atlasapi.feeds.youview.services.BroadcastServiceMapping;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class BroadcastHierarchyExpanderTest {

    private static int DAYS_TO_MAKE_BROADCAST_OLD = 21;

    private Clock clock = new TimeMachine();
    private IdGenerator idGenerator;
    private BroadcastServiceMapping serviceMapping = Mockito.mock(BroadcastServiceMapping.class);
    private NitroServiceIdResolver nitroServiceIdResolver = Mockito.mock(NitroServiceIdResolver.class);
    private BroadcastHierarchyExpander hierarchyExpander;

    @Test
    public void testCombinesEqualIdsInMapping() {
        idGenerator = Mockito.mock(IdGenerator.class);
        hierarchyExpander = new BroadcastHierarchyExpander(idGenerator, serviceMapping,
                nitroServiceIdResolver, clock);
        
        when(idGenerator.generateBroadcastImi(anyString(), any(Broadcast.class))).thenReturn("broadcast_imi");
        when(nitroServiceIdResolver.resolveSId(any(Broadcast.class))).thenReturn(Optional.of("123"));
        when(serviceMapping.youviewServiceIdFor(anyString())).thenReturn(ImmutableSet.of("youviewServiceId"));
        
        Set<Version> versions = createNVersions(3);
        Set<Broadcast> broadcasts = createNBroadcasts(2);
        Item item = createItem(versions, broadcasts);
        Map<String, ItemBroadcastHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(1, hierarchy.size());
    }
    
    @Test
    public void testIfBroadcastImisAreUniqueAllVersionsAreExpanded() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new BroadcastHierarchyExpander(idGenerator, serviceMapping,
                nitroServiceIdResolver, clock);
        
        when(nitroServiceIdResolver.resolveSId(any(Broadcast.class))).thenReturn(Optional.of("123"));
        when(serviceMapping.youviewServiceIdFor(anyString())).thenReturn(ImmutableSet.of("youviewServiceId"));
        
        int numVersions = 3;
        
        Set<Version> versions = createNVersions(numVersions);
        Set<Broadcast> broadcasts = createNBroadcasts(1);
        Item item = createItem(versions, broadcasts);
        Map<String, ItemBroadcastHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(numVersions, hierarchy.size());
    }
    
    @Test
    public void testIfBroadcastImisAreUniqueAllBroadcastsAreExpanded() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new BroadcastHierarchyExpander(idGenerator, serviceMapping,
                nitroServiceIdResolver, clock);
        
        when(nitroServiceIdResolver.resolveSId(any(Broadcast.class))).thenReturn(Optional.of("123"));
        when(serviceMapping.youviewServiceIdFor(anyString())).thenReturn(ImmutableSet.of("youviewServiceId"));
        
        int numNewBroadcasts = 3;
        int numOldBroadcasts = 4;

        Set<Version> versions = createNVersions(1);
        Set<Broadcast> broadcasts = createNewAndOldBroadcasts(numNewBroadcasts, numOldBroadcasts);
        Item item = createItem(versions, broadcasts);
        Map<String, ItemBroadcastHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(numNewBroadcasts, hierarchy.size());
    }
    
    @Test
    public void testIfBroadcastImisUniqueAreUniqueAllYouViewServiceIdsAreExpanded() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new BroadcastHierarchyExpander(idGenerator, serviceMapping,
                nitroServiceIdResolver, clock);
        
        ImmutableSet<String> youViewServiceIds = ImmutableSet.of("yvSID_1", "yvSID_2");
        when(serviceMapping.youviewServiceIdFor(anyString())).thenReturn(youViewServiceIds);
        when(nitroServiceIdResolver.resolveSId(any(Broadcast.class))).thenReturn(Optional.of("123"));
        
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

    private Set<Broadcast> createNewAndOldBroadcasts(int numNewBroadcasts, int numOldBroadcasts) {
        ImmutableSet.Builder<Broadcast> broadcasts = ImmutableSet.builder();

        int count = 0;
        int totalBroadcasts = numNewBroadcasts + numOldBroadcasts;

        for ( ; count < numNewBroadcasts; count++) {
            broadcasts.add(createBroadcast("broadcast_" + count, clock.now()));
        }

        for ( ; count < totalBroadcasts; count++) {
            broadcasts.add(createBroadcast("broadcast_" + count, clock.now().minusDays(DAYS_TO_MAKE_BROADCAST_OLD)));
        }

        return broadcasts.build();
    }

    private Set<Broadcast> createNBroadcasts(int numBroadcasts) {
        ImmutableSet.Builder<Broadcast> broadcasts = ImmutableSet.builder();

        for (int i = 0; i < numBroadcasts; i++) {
            broadcasts.add(createBroadcast("broadcast_" + i, clock.now()));
        }

        return broadcasts.build();
    }

    private Broadcast createBroadcast(String sourceId, DateTime now) {
        Broadcast broadcast = new Broadcast("http://bbc.co.uk/services/bbcone", now, now.plusMinutes(
                30));
        broadcast.withId(sourceId);
        return broadcast;
    }

}
