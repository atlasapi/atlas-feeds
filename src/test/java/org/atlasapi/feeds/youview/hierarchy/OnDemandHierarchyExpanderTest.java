package org.atlasapi.feeds.youview.hierarchy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.atlasapi.feeds.youview.UniqueIdGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.entity.Policy.Platform;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;


public class OnDemandHierarchyExpanderTest {

    private IdGenerator idGenerator;
    private OnDemandHierarchyExpander hierarchyExpander;
    
    @Test
    public void testCombinesEqualIdsInMapping() {
        idGenerator = Mockito.mock(IdGenerator.class);
        hierarchyExpander = new OnDemandHierarchyExpander(idGenerator);
        
        when(idGenerator.generateOnDemandImi(any(Item.class), any(Version.class), any(Encoding.class), any(Location.class))).thenReturn("ondemand_imi");
        
        Set<Version> versions = createNVersions(3);
        Set<Encoding> encodings = createNEncodings(2);
        Set<Location> locations = createNLocations(5);
        Item item = createItem(versions, encodings, locations);
        
        Map<String, ItemOnDemandHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(1, hierarchy.size());
    }

    @Test
    public void testIfOnDemandImisUniqueAreUniqueAllVersionsAreExpanded() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new OnDemandHierarchyExpander(idGenerator);
        
        int numVersions = 3;
        
        Set<Version> versions = createNVersions(numVersions);
        Set<Encoding> encodings = createNEncodings(1);
        Set<Location> locations = createNLocations(1);
        Item item = createItem(versions, encodings, locations);
        
        Map<String, ItemOnDemandHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(numVersions, hierarchy.size());
    }
    
    @Test
    public void testIfOnDemandImisUniqueAreUniqueAllEncodingsAreExpanded() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new OnDemandHierarchyExpander(idGenerator);
        
        int numEncodings = 3;
        
        Set<Version> versions = createNVersions(1);
        Set<Encoding> encodings = createNEncodings(numEncodings);
        Set<Location> locations = createNLocations(1);
        Item item = createItem(versions, encodings, locations);
        
        Map<String, ItemOnDemandHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(numEncodings, hierarchy.size());
    }
    
    @Test
    public void testIfOnDemandImisUniqueAreUniqueAllLocationsAreExpanded() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new OnDemandHierarchyExpander(idGenerator);
        
        int numLocations = 3;
        
        Set<Version> versions = createNVersions(1);
        Set<Encoding> encodings = createNEncodings(1);
        Set<Location> locations = createNLocations(numLocations);
        Item item = createItem(versions, encodings, locations);
        
        Map<String, ItemOnDemandHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(numLocations, hierarchy.size());
    }
    
    @Test
    public void testFiltersNonYouViewIPlayerLocationsFromExpandedHierarchy() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new OnDemandHierarchyExpander(idGenerator);
        
        Set<Version> versions = createNVersions(1);
        Set<Encoding> encodings = createNEncodings(1);
        Set<Location> locations = ImmutableSet.of(createLocationFrom(Platform.XBOX), createLocationFrom(Platform.YOUVIEW_IPLAYER));
        
        Item item = createItem(versions, encodings, locations);
        
        Map<String, ItemOnDemandHierarchy> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(1, hierarchy.size());
    }

    private Location createLocationFrom(Platform platform) {
        Location location = new Location();
        
        Random r = new Random();
        location.setCanonicalUri("location_" + r.nextInt());
        
        Policy policy = new Policy();
        policy.setPlatform(platform);
        
        location.setPolicy(policy);
        
        return location;
    }

    private Item createItem(Set<Version> versions, Set<Encoding> encodings, Set<Location> locations) {
        Item item = new Item("uri", "curie", Publisher.METABROADCAST);
        for (Encoding encoding : encodings) {
            encoding.setAvailableAt(locations);
        }
        for (Version version : versions) {
            version.setManifestedAs(encodings);
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

    private Set<Encoding> createNEncodings(int numEncodings) {
        ImmutableSet.Builder<Encoding> encodings = ImmutableSet.builder();
        for (int i = 0; i < numEncodings; i++) {
            Encoding encoding = new Encoding();
            encoding.setCanonicalUri("encoding_" + i);
            encodings.add(encoding);
        }
        return encodings.build();
    }
    
    private Set<Location> createNLocations(int numLocations) {
        ImmutableSet.Builder<Location> encodings = ImmutableSet.builder();
        for (int i = 0; i < numLocations; i++) {
            Location location = new Location();
            location.setCanonicalUri("location_" + i);
            
            Policy policy = new Policy();
            policy.setPlatform(Platform.YOUVIEW_IPLAYER);
            
            location.setPolicy(policy);
            
            encodings.add(location);
        }
        return encodings.build();
    }
}
