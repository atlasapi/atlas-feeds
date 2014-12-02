package org.atlasapi.feeds.youview.upload;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.scheduling.ScheduledTask.TaskState;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class YouViewUploadTaskTest {

    private static final Publisher PUBLISHER = Publisher.LOVEFILM;
    private static final Publisher ANOTHER_PUBLISHER = Publisher.AMAZON_UNBOX;
    
    private YouViewLastUpdatedStore lastUpdatedStore = Mockito.mock(YouViewLastUpdatedStore.class);
    private YouViewContentResolver contentResolver = Mockito.mock(YouViewContentResolver.class);
    private Clock clock = new TimeMachine();
    private YouViewService youViewClient = Mockito.mock(YouViewService.class);
    
    @Before
    public void setup() {
        when(contentResolver.allContent()).thenReturn(Iterators.<Content>emptyIterator());
        when(contentResolver.updatedSince(any(DateTime.class))).thenReturn(Iterators.<Content>emptyIterator());
    }
    
    @Test
    public void testDeltaWontRunIfNoLastUpdatedRecord() {
        
        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(Optional.<DateTime>absent());
        
        DeltaUploadTask task = new DeltaUploadTask(youViewClient, lastUpdatedStore, PUBLISHER, contentResolver);
        task.run();
        
        verifyZeroInteractions(youViewClient);
        assertEquals(TaskState.FAILED, task.getState());
    }

    @Test
    public void testDeltaWontRunIfNoLastUpdatedRecordForThatPublisher() {
        
        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(Optional.<DateTime>absent());
        when(lastUpdatedStore.getLastUpdated(ANOTHER_PUBLISHER)).thenReturn(Optional.of(clock.now()));
        
        new DeltaUploadTask(youViewClient, lastUpdatedStore, PUBLISHER, contentResolver).run();
        
        verifyZeroInteractions(youViewClient);
    }

    @Test
    public void testBootstrapOnlySetsLastUpdatedForItsOwnPublisher() throws HttpException {
        
        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(Optional.<DateTime>absent());
        
        new BootstrapUploadTask(youViewClient, lastUpdatedStore, PUBLISHER, contentResolver).run();
        
        verify(lastUpdatedStore).setLastUpdated(any(DateTime.class), eq(PUBLISHER));
        verifyNoMoreInteractions(lastUpdatedStore);
    }
}
