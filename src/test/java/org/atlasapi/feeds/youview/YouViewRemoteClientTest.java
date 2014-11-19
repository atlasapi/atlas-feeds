package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmIdGenerator;
import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.feeds.youview.upload.HttpYouViewRemoteClient;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.junit.Before;
import org.junit.Test;

import tva.metadata._2010.ObjectFactory;
import tva.metadata._2010.TVAMainType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
    
    private TvAnytimeGenerator generator = mock(TvAnytimeGenerator.class);
    private SimpleHttpClient httpClient = mock(SimpleHttpClient.class);
    private HttpResponse response = createResponseWithTransaction(TRANSACTION_URL);
    private IdGenerator idGenerator = new LoveFilmIdGenerator();
    
    private final HttpYouViewRemoteClient client = new HttpYouViewRemoteClient(
            generator, 
            httpClient,
            "baseUri",
            idGenerator,
            CLOCK, 
            false
    );
    
    @Before
    public void setup() throws HttpException {
        when(httpClient.post(eq("baseUri/transaction"), any(StringPayload.class))).thenReturn(response);
        when(generator.generateTVAnytimeFrom(any(Content.class))).thenReturn(createTVAElem());
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
        Content contentToUpload = createContentForPublisher(PUBLISHER);
        client.upload(contentToUpload);

        Transaction returnedTxn = client.upload(contentToUpload);
        
        assertEquals(TRANSACTION_URL, returnedTxn.id());
        assertEquals(PUBLISHER, returnedTxn.publisher());
        assertEquals(ImmutableSet.of(contentToUpload.getCanonicalUri()), returnedTxn.content());
        assertEquals(TransactionStateType.ACCEPTED, returnedTxn.status().status());
    }

    private Content createContentForPublisher(Publisher publisher) {
        return new Film("filmUri", "curie", publisher);
    }

}
