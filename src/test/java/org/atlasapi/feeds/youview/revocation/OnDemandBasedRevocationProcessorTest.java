package org.atlasapi.feeds.youview.revocation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;


public class OnDemandBasedRevocationProcessorTest {

    private static final ItemOnDemandHierarchy HIERARCHY = createItemOnDemandHierarchy();
    private static final String ON_DEMAND_IMI = "onDemandImi";
    
    private RevokedContentStore revocationStore = mock(RevokedContentStore.class);
    private OnDemandHierarchyExpander onDemandHierarchyExpander = mock(OnDemandHierarchyExpander.class);
    private GranularYouViewService youviewService = mock(GranularYouViewService.class);
    
    private final RevocationProcessor processor;
    
    public OnDemandBasedRevocationProcessorTest() {
        when(onDemandHierarchyExpander.expandHierarchy(HIERARCHY.item())).thenReturn(ImmutableMap.of(ON_DEMAND_IMI, HIERARCHY));
        this.processor = new OnDemandBasedRevocationProcessor(revocationStore, onDemandHierarchyExpander, youviewService);
    }
    
    @Test
    public void testRevokeStoresRevokedContentUriAndSendsOnDemandDeletes() {
        processor.revoke(HIERARCHY.item());
        
        verify(youviewService).sendDeleteFor(HIERARCHY.item(), TVAElementType.ONDEMAND, ON_DEMAND_IMI);
        verify(revocationStore).revoke(HIERARCHY.item().getCanonicalUri());
    }

    @Test
    public void testUnrevokeRemovesRevokedContentUriAndSendsOnDemandUploads() {
        processor.unrevoke(HIERARCHY.item());
        
        verify(revocationStore).unrevoke(HIERARCHY.item().getCanonicalUri());
        verify(youviewService).uploadOnDemand(HIERARCHY, ON_DEMAND_IMI);
    }

    private static ItemOnDemandHierarchy createItemOnDemandHierarchy() {
        return new ItemOnDemandHierarchy(createItem(), createVersion(), createEncoding(), createLocation());
    }

    private static Item createItem() {
        return new Film("film", "curie", Publisher.METABROADCAST);
    }

    private static Version createVersion() {
        return new Version();
    }

    private static Encoding createEncoding() {
        return new Encoding();
    }

    private static Location createLocation() {
        return new Location();
    }

}
