package org.atlasapi.feeds.youview.upload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Test;


public class RevocationHonouringYouViewServiceTest {

    private GranularYouViewService delegate = mock(GranularYouViewService.class);
    private RevokedContentStore revocationStore = mock(RevokedContentStore.class);
    
    private final RevocationHonouringYouViewService service = new RevocationHonouringYouViewService(revocationStore, delegate);
    
    @Test
    public void testUnrevokedContentUploadedSuccessfully() {
        Item item = createFilm();
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(false);
        
        service.uploadContent(item);
        
        verify(delegate).uploadContent(item);
    }
    
    @Test
    public void testRevokedContentNotUploaded() {
        Item item = createFilm();
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(true);
        
        service.uploadContent(item);
        
        verifyZeroInteractions(delegate);
    }
    
    @Test
    public void testVersionUploadedSuccessfullyIfContentNotRevoked() {
        Item item = createFilm();
        Version version = createVersion();
        ItemAndVersion versionHierarchy = new ItemAndVersion(item, version);
        String versionCrid = "versionCrid";
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(false);
        
        service.uploadVersion(versionHierarchy, versionCrid);
        
        verify(delegate).uploadVersion(versionHierarchy, versionCrid);
    }

    @Test
    public void testVersionNotUploadedIfContentIsRevoked() {
        Item item = createFilm();
        Version version = createVersion();
        ItemAndVersion versionHierarchy = new ItemAndVersion(item, version);
        String versionCrid = "versionCrid";
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(true);
        
        service.uploadVersion(versionHierarchy, versionCrid);
        
        verifyZeroInteractions(delegate);
    }
    
    @Test
    public void testBroadcastUploadedSuccessfullyIfContentNotRevoked() {
        Item item = createFilm();
        Version version = createVersion();
        Broadcast bcast = createBroadcast();
        ItemBroadcastHierarchy broadcastHierarchy = new ItemBroadcastHierarchy(item, version, bcast, "serviceId");
        String broadcastImi = "broadcastImi";
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(false);
        
        service.uploadBroadcast(broadcastHierarchy, broadcastImi);
        
        verify(delegate).uploadBroadcast(broadcastHierarchy, broadcastImi);
    }

    @Test
    public void testBroadcastNotUploadedIfContentIsRevoked() {
        Item item = createFilm();
        Version version = createVersion();
        Broadcast bcast = createBroadcast();
        ItemBroadcastHierarchy broadcastHierarchy = new ItemBroadcastHierarchy(item, version, bcast, "serviceId");
        String broadcastImi = "broadcastImi";
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(true);
        
        service.uploadBroadcast(broadcastHierarchy, broadcastImi);
        
        verifyZeroInteractions(delegate);
    }
    
    @Test
    public void testOnDemandUploadedSuccessfullyIfContentNotRevoked() {
        Item item = createFilm();
        Version version = createVersion();
        Encoding encoding = createEncoding();
        Location location = createLocation();
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(item, version, encoding, location);
        String onDemandImi = "onDemandImi";
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(false);

        service.uploadOnDemand(onDemandHierarchy, onDemandImi);
        
        verify(delegate).uploadOnDemand(onDemandHierarchy, onDemandImi);
    }

    @Test
    public void testOnDemandNotUploadedIfContentIsRevoked() {
        Item item = createFilm();
        Version version = createVersion();
        Encoding encoding = createEncoding();
        Location location = createLocation();
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(item, version, encoding, location);
        String onDemandImi = "onDemandImi";
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(true);
        
        service.uploadOnDemand(onDemandHierarchy, onDemandImi);
        
        verifyZeroInteractions(delegate);
    }
    
    @Test
    public void testElementDeletedSuccessfullyIfContentNotRevoked() {
        Item item = createFilm();
        TVAElementType type = TVAElementType.ONDEMAND;
        String onDemandImi = "onDemandImi";
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(false);

        service.sendDeleteFor(item, type, onDemandImi);
        
        verify(delegate).sendDeleteFor(item, type, onDemandImi);
    }

    @Test
    public void testElementNotDeletedIfContentIsRevoked() {
        Item item = createFilm();
        TVAElementType type = TVAElementType.ONDEMAND;
        String onDemandImi = "onDemandImi";
        
        when(revocationStore.isRevoked(item.getCanonicalUri())).thenReturn(true);
        
        service.sendDeleteFor(item, type, onDemandImi);
        
        verifyZeroInteractions(delegate);
    }

    @Test
    public void testRemoteCheckDoesntCheckRevocations() {
        Task task = createTask();
        
        service.checkRemoteStatusOf(task);
        
        verify(delegate).checkRemoteStatusOf(task);
        
        verifyZeroInteractions(revocationStore);
    }

    private Task createTask() {
        return mock(Task.class);
    }

    private Item createFilm() {
        return new Film("film", "curie", Publisher.METABROADCAST);
    }
    
    private Version createVersion() {
        return new Version();
    }

    private Broadcast createBroadcast() {
        return new Broadcast("bbc.co.uk/services/bbcone", new DateTime(), new DateTime().plusMinutes(30));
    }

    private Encoding createEncoding() {
        return new Encoding();
    }

    private Location createLocation() {
        return new Location();
    }
}
