package org.atlasapi.feeds.youview.hierarchy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.atlasapi.feeds.youview.UniqueIdGenerator;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;


public class VersionHierarchyExpanderTest {

    private IdGenerator idGenerator;
    private VersionHierarchyExpander hierarchyExpander;
    
    @Test
    public void testCombinesEqualIdsInMapping() {
        idGenerator = Mockito.mock(IdGenerator.class);
        hierarchyExpander = new VersionHierarchyExpander(idGenerator);
        
        when(idGenerator.generateVersionCrid(any(Item.class), any(Version.class))).thenReturn("version_crid");
        
        Set<Version> versions = createNVersions(3);
        Item item = createItem(versions);
        
        Map<String, ItemAndVersion> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(1, hierarchy.size());
    }

    @Test
    public void testIfVersionCridsUniqueAreUniqueAllVersionsAreExpanded() {
        idGenerator = new UniqueIdGenerator();
        hierarchyExpander = new VersionHierarchyExpander(idGenerator);
        
        int numVersions = 3;
        
        Set<Version> versions = createNVersions(numVersions);
        Item item = createItem(versions);
        
        Map<String, ItemAndVersion> hierarchy = hierarchyExpander.expandHierarchy(item);
        
        assertEquals(numVersions, hierarchy.size());
    }

    private Item createItem(Set<Version> versions) {
        Item item = new Item("uri", "curie", Publisher.METABROADCAST);
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
}
