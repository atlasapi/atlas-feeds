package org.atlasapi.feeds.youview.revocation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.creation.TaskCreator;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;


public class OnDemandBasedRevocationProcessorTest {

    private static final ItemOnDemandHierarchy HIERARCHY = createItemOnDemandHierarchy();
    private static final String ON_DEMAND_IMI = "onDemandImi";
    private static final Long TASK_ID = 1l;
    
    private RevokedContentStore revocationStore = mock(RevokedContentStore.class);
    private OnDemandHierarchyExpander onDemandHierarchyExpander = mock(OnDemandHierarchyExpander.class);
    
    private Task task = mock(Task.class);
    private Payload payload = createPayload();
    private PayloadCreator payloadCreator = mock(PayloadCreator.class);
    private TaskCreator taskCreator = mock(TaskCreator.class);
    private TaskStore taskStore = mock(TaskStore.class);
    
    private final RevocationProcessor processor = new OnDemandBasedRevocationProcessor(revocationStore, payloadCreator, taskCreator, taskStore);;
    
    @Before
    public void setup() throws PayloadGenerationException {
        when(onDemandHierarchyExpander.expandHierarchy(HIERARCHY.item())).thenReturn(ImmutableMap.of(ON_DEMAND_IMI, HIERARCHY));
        when(payloadCreator.payloadFrom(ON_DEMAND_IMI, HIERARCHY)).thenReturn(payload);
        when(task.id()).thenReturn(TASK_ID);
    }

    @Test
    public void testRevokeStoresRevokedContentUriAndSendsOnDemandDeletes() {
        when(taskStore.save(task)).thenReturn(task);
        when(taskCreator.deleteFor(ON_DEMAND_IMI, HIERARCHY)).thenReturn(task);
        
        processor.revoke(HIERARCHY.item());
        
        verify(taskStore).save(task);
        verify(revocationStore).revoke(HIERARCHY.item().getCanonicalUri());
    }

    @Test
    public void testUnrevokeRemovesRevokedContentUriAndSendsOnDemandUploads() {
        when(taskCreator.taskFor(ON_DEMAND_IMI, HIERARCHY, payload, Action.UPDATE)).thenReturn(task);
        when(taskStore.save(task)).thenReturn(task);
        
        processor.unrevoke(HIERARCHY.item());
        
        verify(revocationStore).unrevoke(HIERARCHY.item().getCanonicalUri());
        verify(taskStore).save(task);
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
    
    private Payload createPayload() {
        return new Payload("", new DateTime());
    }
}
