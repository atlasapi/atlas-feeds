package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TVAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.genres.GenreMappings;
import org.atlasapi.feeds.youview.ids.IdParsers;
import org.atlasapi.feeds.youview.ids.PublisherIdUtilities;
import org.atlasapi.feeds.youview.images.ImageConfigurations;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.transactions.TransactionStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.Payload;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class YouViewLastUpdatedStoreTest {

    private LastUpdatedContentFinder lastUpdatedContentFinder = Mockito.mock(LastUpdatedContentFinder.class);
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private YouViewPerPublisherFactory configFactory = YouViewPerPublisherFactory.builder()
            .withPublisher(
                    Publisher.LOVEFILM, 
                    PublisherIdUtilities.idUtilFor(Publisher.LOVEFILM, "baseUri"),
                    ImageConfigurations.imageConfigFor(Publisher.LOVEFILM),
                    IdParsers.parserFor(Publisher.LOVEFILM), 
                    GenreMappings.mappingFor(Publisher.LOVEFILM), 
                    httpClient,
                    Mockito.mock(TransactionStore.class))
            .build();
    private TVAnytimeElementCreator elementCreator = Mockito.mock(TVAnytimeElementCreator.class);
    private TvAnytimeGenerator generator = new DefaultTvAnytimeGenerator(elementCreator, false);
    private YouViewRemoteClient youViewClient = new YouViewRemoteClient(generator, configFactory);
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private final YouViewLastUpdatedStore store = new MongoYouViewLastUpdatedStore(mongo);
    
    @Test(expected = RuntimeException.class)
    public void testDeltaWontRunIfNoLastUpdatedRecord() throws HttpException {
        YouViewUploadTask task = new YouViewUploadTask(youViewClient, 1, lastUpdatedContentFinder, store, Publisher.LOVEFILM, false);
        task.run();
        Mockito.verifyZeroInteractions(lastUpdatedContentFinder.updatedSince(Publisher.LOVEFILM, Mockito.any(DateTime.class)));
        Mockito.verifyZeroInteractions(httpClient.post(Mockito.anyString(), Mockito.any(Payload.class)));
        Mockito.verifyZeroInteractions(httpClient.delete(Mockito.anyString()));
    }

    @Test(expected = RuntimeException.class)
    public void testDeltaWontRunIfNoLastUpdatedRecordForThatPublisher() throws HttpException {
        store.setLastUpdated(DateTime.now().minusDays(2), Publisher.AMAZON_UNBOX);
        
        YouViewUploadTask task = new YouViewUploadTask(youViewClient, 1, lastUpdatedContentFinder, store, Publisher.LOVEFILM, false);
        task.run();
        Mockito.verifyZeroInteractions(lastUpdatedContentFinder.updatedSince(Publisher.LOVEFILM, Mockito.any(DateTime.class)));
        Mockito.verifyZeroInteractions(httpClient.post(Mockito.anyString(), Mockito.any(Payload.class)));
        Mockito.verifyZeroInteractions(httpClient.delete(Mockito.anyString()));
    }

    @Test(expected = RuntimeException.class)
    public void testBootstrapOnlySetsLastUpdatedForItsOwnPublisher() throws HttpException {
        
        assertFalse(store.getLastUpdated(Publisher.LOVEFILM).isPresent());
        assertFalse(store.getLastUpdated(Publisher.AMAZON_UNBOX).isPresent());
        
        YouViewUploadTask task = new YouViewUploadTask(youViewClient, 1, lastUpdatedContentFinder, store, Publisher.LOVEFILM, true);
        task.run();
        Mockito.when(lastUpdatedContentFinder.updatedSince(Publisher.LOVEFILM, Mockito.any(DateTime.class))).thenReturn(ImmutableList.<Content>of().iterator());
      
        assertTrue(store.getLastUpdated(Publisher.LOVEFILM).isPresent());
        assertFalse(store.getLastUpdated(Publisher.AMAZON_UNBOX).isPresent());
    }
}
