package org.atlasapi.feeds.youview;

import java.io.UnsupportedEncodingException;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.ids.PublisherIdUtilities;
import org.atlasapi.feeds.youview.ids.PublisherIdUtility;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.time.TimeMachine;


public class YouViewRemoteClientTest {

    private static final String TRANSACTION_URL = "transactionUrl";
    private static final Publisher PUBLISHER = Publisher.LOVEFILM;
    
    private TvAnytimeGenerator generator = Mockito.mock(TvAnytimeGenerator.class);
    private YouViewPerPublisherFactory publisherFactory = Mockito.mock(YouViewPerPublisherFactory.class);
    private TransactionStore transactionStore = Mockito.mock(TransactionStore.class);
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private PublisherIdUtility lovefilmIdUtil = PublisherIdUtilities.idUtilFor(PUBLISHER, "baseUri");
    private HttpResponse response = createResponseWithTransaction(TRANSACTION_URL);
    private final YouViewRemoteClient client = new YouViewRemoteClient(generator, publisherFactory, transactionStore, new TimeMachine());
    
    @Before
    public void setup() throws HttpException {
        Mockito.when(publisherFactory.getIdUtil(PUBLISHER)).thenReturn(lovefilmIdUtil);
        Mockito.when(publisherFactory.getHttpClient(PUBLISHER)).thenReturn(httpClient);
        Mockito.when(httpClient.post(Mockito.eq("baseUri/transaction"), Mockito.any(StringPayload.class))).thenReturn(response);
    }
    
    private HttpResponse createResponseWithTransaction(String transactionUrl) {
        return new HttpResponse("", 202, "SUCCESSED", ImmutableMap.of("Location", transactionUrl));
    }

    @Ignore // ignored until transaction saving code is reimplemented with the refactored txn store
    @Test
    public void testThatReturnedTransactionIdsAreStored() throws UnsupportedEncodingException, HttpException {
        ImmutableList<Content> contentToUpload = ImmutableList.of(createContentForPublisher(PUBLISHER));
        client.upload(contentToUpload);

//        Mockito.verify(transactionStore).save(Mockito.eq(TRANSACTION_URL), Mockito.<Map<Content, Duration>>any());
    }
    

    private Content createContentForPublisher(Publisher publisher) {
        return new Film("filmUri", "curie", publisher);
    }

}
