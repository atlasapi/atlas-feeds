package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.JaxbTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmGroupInformationHierarchyTest.DummyContentResolver;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmIdGenerator;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.feeds.youview.upload.HttpYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.Payload;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.time.TimeMachine;
import com.metabroadcast.common.url.UrlEncoding;


public class SingleItemEndpointTest {

    private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private DummyContentResolver contentResolver = new DummyContentResolver();
    private ContentPermit contentPermit = Mockito.mock(ContentPermit.class);
    private TvAnytimeElementCreator elementCreator = Mockito.mock(TvAnytimeElementCreator.class);
    private TvAnytimeGenerator generator = new JaxbTvAnytimeGenerator(elementCreator);
    private IdGenerator idGenerator = new LoveFilmIdGenerator();
    private YouViewRemoteClient youViewClient = new HttpYouViewRemoteClient(
            generator, 
            httpClient,
            "youviewurl",
            idGenerator,
            new TimeMachine(), 
            false
    );
    private LastUpdatedContentFinder contentFinder = Mockito.mock(LastUpdatedContentFinder.class);
    
    private final YouViewUploadController controller = new YouViewUploadController(contentFinder, contentResolver, youViewClient, Mockito.mock(TransactionStore.class));
    
    @Before
    public void setup() throws HttpException, IOException {
        HttpResponse httpResponse = new HttpResponse("", HttpServletResponse.SC_ACCEPTED, "", ImmutableMap.of("Location", "yv location"));
        Mockito.when(httpClient.delete(Mockito.anyString())).thenReturn(httpResponse);
        Mockito.when(httpClient.post(Mockito.anyString(), Mockito.any(Payload.class))).thenReturn(httpResponse);
        Mockito.when(response.getOutputStream()).thenReturn(Mockito.mock(ServletOutputStream.class));
        Mockito.when(elementCreator.permit()).thenReturn(contentPermit);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDeleteFailsOnBadPublisher() throws HttpException, IOException {
        contentResolver.addContent(createItem("itemUri", "itemASIN"));

        controller.deleteContent(response, "lurvefilm", "itemUri");

        Mockito.verifyZeroInteractions(httpClient);
    }
    
    // ignored until deletes are refactored
    @Ignore
    @Test
    public void testDeleteCalledByDeletionEndpoint() throws HttpException, IOException {
        contentResolver.addContent(createItem("http://lovefilm.com/episodes/item", "itemASIN"));

        controller.deleteContent(response, "lovefilm", "http://lovefilm.com/episodes/item");

        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/item"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/item_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/item"));
    }
    
    @Ignore // TODO ignored until problem of leaking marshaller and JAXB context into multiple locations is fixed
    @Test
    public void testUploadPerformedByUploadEndpoint() throws IOException, HttpException {
        Item item = createItem("http://lovefilm.com/episodes/item", "itemASIN");
        contentResolver.addContent(item);

        controller.uploadContent(response, "lovefilm", "http://lovefilm.com/episodes/item");

        ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);
        
        Mockito.verify(httpClient).post(Mockito.eq("youviewurl/transaction"), payloadCaptor.capture());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        generator.generateXml(ImmutableList.<Content>of(item), baos);
        String expected = baos.toString(Charsets.UTF_8.name());
        
        StringPayload payload = (StringPayload) payloadCaptor.getValue();
        
        assertEquals(new StringPayload(expected), payload);
    }

    private Item createItem(String uri, String asin) {
        Item item = new Item();
        item.setPublisher(Publisher.LOVEFILM);
        item.setCanonicalUri(uri);
        item.addAlias(new Alias("gb:amazon:asin", asin));
        item.setLastUpdated(new DateTime());
        return item;
    }
}
