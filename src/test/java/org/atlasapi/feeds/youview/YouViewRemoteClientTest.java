package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.ids.PublisherIdUtilities;
import org.atlasapi.feeds.youview.ids.PublisherIdUtility;
import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import tva.metadata._2010.ObjectFactory;
import tva.metadata._2010.TVAMainType;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class YouViewRemoteClientTest {

    private static final Clock CLOCK = new TimeMachine();
    private static final String TRANSACTION_URL = "transactionUrl";
    private static final Publisher PUBLISHER = Publisher.LOVEFILM;
    
    private TvAnytimeGenerator generator = Mockito.mock(TvAnytimeGenerator.class);
    private YouViewPerPublisherFactory publisherFactory = Mockito.mock(YouViewPerPublisherFactory.class);
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private PublisherIdUtility lovefilmIdUtil = PublisherIdUtilities.idUtilFor(PUBLISHER, "baseUri");
    private HttpResponse response = createResponseWithTransaction(TRANSACTION_URL);
    
    private final YouViewRemoteClient client = new YouViewRemoteClient(
            generator, 
            publisherFactory, 
            CLOCK, 
            false
    );
    
    @Before
    public void setup() throws HttpException {
        Mockito.when(publisherFactory.getIdUtil(PUBLISHER)).thenReturn(lovefilmIdUtil);
        Mockito.when(publisherFactory.getHttpClient(PUBLISHER)).thenReturn(httpClient);
        Mockito.when(httpClient.post(Mockito.eq("baseUri/transaction"), Mockito.any(StringPayload.class))).thenReturn(response);
        Mockito.when(generator.generateTVAnytimeFrom(Mockito.anyCollectionOf(Content.class))).thenReturn(createTVAElem());
    }
    
    private JAXBElement<TVAMainType> createTVAElem() {
        ObjectFactory factory = new ObjectFactory();
        TVAMainType tvaMain = factory.createTVAMainType();
        return factory.createTVAMain(tvaMain);
    }

    private HttpResponse createResponseWithTransaction(String transactionUrl) {
        return new HttpResponse("", 202, "SUCCESSED", ImmutableMap.of("Location", transactionUrl));
    }

    @Test
    public void testThatUploadingSuccessfullyReturnsTransaction() throws UnsupportedEncodingException, HttpException {
        ImmutableList<Content> contentToUpload = ImmutableList.of(createContentForPublisher(PUBLISHER));
        client.upload(contentToUpload);

        Transaction returnedTxn = client.upload(contentToUpload).get();
        
        assertEquals(TRANSACTION_URL, returnedTxn.id());
        assertEquals(PUBLISHER, returnedTxn.publisher());
        assertEquals(ImmutableSet.copyOf(urlsFrom(contentToUpload)), returnedTxn.content());
        assertEquals(TransactionStateType.ACCEPTED, returnedTxn.status().status());
    }

    private Iterable<String> urlsFrom(ImmutableList<Content> content) {
        return Iterables.transform(content, new Function<Content, String>() {
            @Override
            public String apply(Content input) {
                return input.getCanonicalUri();
            }
        });
    }

    private Content createContentForPublisher(Publisher publisher) {
        return new Film("filmUri", "curie", publisher);
    }

}
