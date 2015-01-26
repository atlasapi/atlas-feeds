package org.atlasapi.feeds.youview.tasks.creation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.junit.Before;
import org.junit.Test;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class PayloadCreatingTaskCreatorTest {

    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final Version VERSION = mock(Version.class);
    private static final Broadcast BROADCAST = mock(Broadcast.class);
    private static final Encoding ENCODING = mock(Encoding.class);
    private static final Location LOCATION = mock(Location.class);
    
    private Clock clock = new TimeMachine();
    private Payload payload = createPayload();
    private PayloadCreator payloadCreator = mock(PayloadCreator.class);
    
    private final TaskCreator creator = new YouViewEntityTaskCreator(payloadCreator);
    
    @Before
    public void setup() {
        when(payloadCreator.payloadFrom(any(Content.class))).thenReturn(payload);
        when(payloadCreator.payloadFrom(anyString(), any(ItemAndVersion.class))).thenReturn(payload);
        when(payloadCreator.payloadFrom(anyString(), any(ItemBroadcastHierarchy.class))).thenReturn(payload);
        when(payloadCreator.payloadFrom(anyString(), any(ItemOnDemandHierarchy.class))).thenReturn(payload);
    }

    @Test
    public void testCreatesAppropriateTaskForContentUpdate() {
        String contentCrid = "contentCrid";
        Content content = createBrand();
        Action update = Action.UPDATE;
        
        Task createdTask = creator.create(contentCrid, content, update);
        
        assertEquals(content.getCanonicalUri(), createdTask.content());
        assertEquals(content.getPublisher(), createdTask.publisher());
        assertEquals(TVAElementType.BRAND, createdTask.elementType());
        assertEquals(contentCrid, createdTask.elementId());
        assertEquals(payload, createdTask.payload());
        assertEquals(Status.NEW, createdTask.status());
        
        verify(payloadCreator).payloadFrom(content);
    }
    
    @Test
    public void testCreatesAppropriateTaskForVersionUpdate() {
        String versionCrid = "versionCrid";
        Item item = createFilm();
        ItemAndVersion versionHierarchy = new ItemAndVersion(item, VERSION);
        Action update = Action.UPDATE;
        
        Task createdTask = creator.create(versionCrid, versionHierarchy, update);
        
        assertEquals(item.getCanonicalUri(), createdTask.content());
        assertEquals(item.getPublisher(), createdTask.publisher());
        assertEquals(TVAElementType.VERSION, createdTask.elementType());
        assertEquals(versionCrid, createdTask.elementId());
        assertEquals(payload, createdTask.payload());
        assertEquals(Status.NEW, createdTask.status());
        
        verify(payloadCreator).payloadFrom(versionCrid, versionHierarchy);
    }
    
    @Test
    public void testCreatesAppropriateTaskForBroadcastUpdate() {
        String broadcastImi = "broadcastImi";
        Item item = createFilm();
        ItemBroadcastHierarchy broadcastHierarchy = new ItemBroadcastHierarchy(item, VERSION, BROADCAST, "serviceId");
        Action update = Action.UPDATE;
        
        Task createdTask = creator.create(broadcastImi, broadcastHierarchy, update);
        
        assertEquals(item.getCanonicalUri(), createdTask.content());
        assertEquals(item.getPublisher(), createdTask.publisher());
        assertEquals(TVAElementType.BROADCAST, createdTask.elementType());
        assertEquals(broadcastImi, createdTask.elementId());
        assertEquals(payload, createdTask.payload());
        assertEquals(Status.NEW, createdTask.status());
        
        verify(payloadCreator).payloadFrom(broadcastImi, broadcastHierarchy);
    }
    
    @Test
    public void testCreatesAppropriateTaskForOnDemandUpdate() {
        String onDemandImi = "onDemandImi";
        Item item = createFilm();
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(item, VERSION, ENCODING, LOCATION);
        Action update = Action.UPDATE;
        
        Task createdTask = creator.create(onDemandImi, onDemandHierarchy, update);
        
        assertEquals(item.getCanonicalUri(), createdTask.content());
        assertEquals(item.getPublisher(), createdTask.publisher());
        assertEquals(TVAElementType.ONDEMAND, createdTask.elementType());
        assertEquals(onDemandImi, createdTask.elementId());
        assertEquals(payload, createdTask.payload());
        assertEquals(Status.NEW, createdTask.status());
        
        verify(payloadCreator).payloadFrom(onDemandImi, onDemandHierarchy);
    }

    private Content createBrand() {
        return new Brand("brand", "brand", PUBLISHER);
    }
    
    private Item createFilm() {
        return new Film("film", "film", PUBLISHER);
    }
    
    private Payload createPayload() {
        return new Payload("payload", clock.now());
    }
}
