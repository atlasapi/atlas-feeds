package org.atlasapi.feeds.youview.upload;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
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
    
    private LastUpdatedContentFinder lastUpdatedContentFinder = Mockito.mock(LastUpdatedContentFinder.class);
    private YouViewLastUpdatedStore lastUpdatedStore = Mockito.mock(YouViewLastUpdatedStore.class);
    private Clock clock = new TimeMachine();
    private YouViewClient youViewClient = Mockito.mock(YouViewClient.class);
    
    @Before
    public void setup() {
        when(lastUpdatedContentFinder.updatedSince(any(Publisher.class), any(DateTime.class))).thenReturn(Iterators.<Content>emptyIterator());
    }
    
    @Test
    public void testDeltaWontRunIfNoLastUpdatedRecord() {
        
        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(Optional.<DateTime>absent());
        
        DeltaUploadTask task = new DeltaUploadTask(youViewClient, lastUpdatedContentFinder, lastUpdatedStore, PUBLISHER);
        task.run();
        
        verifyZeroInteractions(youViewClient);
        assertEquals(TaskState.FAILED, task.getState());
    }

    @Test
    public void testDeltaWontRunIfNoLastUpdatedRecordForThatPublisher() {
        
        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(Optional.<DateTime>absent());
        when(lastUpdatedStore.getLastUpdated(ANOTHER_PUBLISHER)).thenReturn(Optional.of(clock.now()));
        
        new DeltaUploadTask(youViewClient, lastUpdatedContentFinder, lastUpdatedStore, PUBLISHER).run();
        
        verifyZeroInteractions(youViewClient);
    }

    @Test
    public void testBootstrapOnlySetsLastUpdatedForItsOwnPublisher() throws HttpException {
        
        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(Optional.<DateTime>absent());
        
        new BootstrapUploadTask(youViewClient, lastUpdatedContentFinder, lastUpdatedStore, PUBLISHER).run();
        
        verify(lastUpdatedStore).setLastUpdated(any(DateTime.class), eq(PUBLISHER));
        verifyNoMoreInteractions(lastUpdatedStore);
    }
}
