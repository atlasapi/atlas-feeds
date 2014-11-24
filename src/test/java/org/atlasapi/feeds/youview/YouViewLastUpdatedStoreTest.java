package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.atlasapi.feeds.tvanytime.JaxbTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmIdGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.upload.HttpYouViewClient;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.Payload;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.TimeMachine;


@Ignore // TODO fix these tests
// TODO this isn't actually testing the store, it's testing the behaviour of the tasks with respect to the store
public class YouViewLastUpdatedStoreTest {

    private LastUpdatedContentFinder lastUpdatedContentFinder = Mockito.mock(LastUpdatedContentFinder.class);
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private TvAnytimeElementCreator elementCreator = Mockito.mock(TvAnytimeElementCreator.class);
    private TvAnytimeGenerator generator = new JaxbTvAnytimeGenerator(elementCreator);
    private IdGenerator idGenerator = new LoveFilmIdGenerator();
//    private HttpYouViewClient youViewClient = new HttpYouViewClient(
//            generator, 
//            httpClient,
//            "baseUri",
//            idGenerator,
//            new TimeMachine(), 
//            false
//    );
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private final YouViewLastUpdatedStore store = new MongoYouViewLastUpdatedStore(mongo);
    
    @Test(expected = RuntimeException.class)
    public void testDeltaWontRunIfNoLastUpdatedRecord() throws HttpException {
//        YouViewUploadTask task = new YouViewUploadTask(youViewClient, lastUpdatedContentFinder, store, Publisher.LOVEFILM, false, Mockito.mock(TaskStore.class));
//        task.run();
        Mockito.verifyZeroInteractions(lastUpdatedContentFinder.updatedSince(Publisher.LOVEFILM, Mockito.any(DateTime.class)));
        Mockito.verifyZeroInteractions(httpClient.post(Mockito.anyString(), Mockito.any(Payload.class)));
        Mockito.verifyZeroInteractions(httpClient.delete(Mockito.anyString()));
    }

    @Test(expected = RuntimeException.class)
    public void testDeltaWontRunIfNoLastUpdatedRecordForThatPublisher() throws HttpException {
        store.setLastUpdated(DateTime.now().minusDays(2), Publisher.AMAZON_UNBOX);
        
//        YouViewUploadTask task = new YouViewUploadTask(youViewClient, lastUpdatedContentFinder, store, Publisher.LOVEFILM, false, Mockito.mock(TaskStore.class));
//        task.run();
        Mockito.verifyZeroInteractions(lastUpdatedContentFinder.updatedSince(Publisher.LOVEFILM, Mockito.any(DateTime.class)));
        Mockito.verifyZeroInteractions(httpClient.post(Mockito.anyString(), Mockito.any(Payload.class)));
        Mockito.verifyZeroInteractions(httpClient.delete(Mockito.anyString()));
    }

    @Test(expected = RuntimeException.class)
    public void testBootstrapOnlySetsLastUpdatedForItsOwnPublisher() throws HttpException {
        
        assertFalse(store.getLastUpdated(Publisher.LOVEFILM).isPresent());
        assertFalse(store.getLastUpdated(Publisher.AMAZON_UNBOX).isPresent());
        
//        YouViewUploadTask task = new YouViewUploadTask(youViewClient, lastUpdatedContentFinder, store, Publisher.LOVEFILM, true, Mockito.mock(TaskStore.class));
//        task.run();
        Mockito.when(lastUpdatedContentFinder.updatedSince(Publisher.LOVEFILM, Mockito.any(DateTime.class))).thenReturn(ImmutableList.<Content>of().iterator());
      
        assertTrue(store.getLastUpdated(Publisher.LOVEFILM).isPresent());
        assertFalse(store.getLastUpdated(Publisher.AMAZON_UNBOX).isPresent());
    }
}
