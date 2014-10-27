package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TVAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.LoveFilmGroupInformationHierarchyTest.DummyContentResolver;
import org.atlasapi.feeds.youview.genres.GenreMappings;
import org.atlasapi.feeds.youview.ids.IdParsers;
import org.atlasapi.feeds.youview.ids.PublisherIdUtilities;
import org.atlasapi.feeds.youview.images.ImageConfigurations;
import org.atlasapi.feeds.youview.transactions.TransactionStore;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.Payload;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.url.UrlEncoding;


public class SingleItemEndpointTest {

    private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private YouViewPerPublisherFactory configFactory = YouViewPerPublisherFactory.builder()
            .withPublisher(
                    Publisher.LOVEFILM, 
                    PublisherIdUtilities.idUtilFor(Publisher.LOVEFILM, "youviewurl"),
                    ImageConfigurations.imageConfigFor(Publisher.LOVEFILM),
                    IdParsers.parserFor(Publisher.LOVEFILM), 
                    GenreMappings.mappingFor(Publisher.LOVEFILM), 
                    httpClient,
                    Mockito.mock(TransactionStore.class))
            .build();
    private ProgramInformationGenerator progInfoGenerator = new DefaultProgramInformationGenerator(configFactory);
    private GroupInformationGenerator groupInfoGenerator = new DefaultGroupInformationGenerator(configFactory);
    private OnDemandLocationGenerator progLocationGenerator = new DefaultOnDemandLocationGenerator(configFactory);
    private DummyContentResolver contentResolver = new DummyContentResolver();
    private ContentHierarchyExtractor hierarchy = new ContentResolvingContentHierarchyExtractor(contentResolver);
    private TVAnytimeElementCreator elementCreator = new DefaultTvAnytimeElementCreator(
            progInfoGenerator, 
            groupInfoGenerator, 
            progLocationGenerator, 
            hierarchy,
            new UriBasedContentPermit()
    );
    private TvAnytimeGenerator generator = new DefaultTvAnytimeGenerator(elementCreator, false);
    YouViewRemoteClient youViewClient = new YouViewRemoteClient(generator, configFactory);
    private LastUpdatedContentFinder contentFinder = Mockito.mock(LastUpdatedContentFinder.class);
    
    private final YouViewUploadController controller = new YouViewUploadController(contentFinder, contentResolver, youViewClient);
    
    @Before
    public void setup() throws HttpException, IOException {
        HttpResponse httpResponse = new HttpResponse("", HttpServletResponse.SC_ACCEPTED, "", ImmutableMap.of("Location", "yv location"));
        Mockito.when(httpClient.delete(Mockito.anyString())).thenReturn(httpResponse);
        Mockito.when(httpClient.post(Mockito.anyString(), Mockito.any(Payload.class))).thenReturn(httpResponse);
        Mockito.when(response.getOutputStream()).thenReturn(Mockito.mock(ServletOutputStream.class));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDeleteFailsOnBadPublisher() throws HttpException, IOException {
        contentResolver.addContent(createItem("itemUri", "itemASIN"));

        controller.deleteContent(response, "lurvefilm", "itemUri");

        Mockito.verifyZeroInteractions(httpClient);
    }
    
    @Test
    public void testDeleteCalledByDeletionEndpoint() throws HttpException, IOException {
        contentResolver.addContent(createItem("http://lovefilm.com/episodes/item", "itemASIN"));

        controller.deleteContent(response, "lovefilm", "http://lovefilm.com/episodes/item");

        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/item"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/item_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/item"));
    }
    
    @Test
    public void testUploadPerformedByUploadEndpoint() throws IOException, HttpException {
        Item item = createItem("http://lovefilm.com/episodes/item", "itemASIN");
        contentResolver.addContent(item);

        controller.uploadContent(response, "lovefilm", "http://lovefilm.com/episodes/item");

        ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);
        
        Mockito.verify(httpClient).post(Mockito.eq("youviewurl/transaction"), payloadCaptor.capture());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        generator.generateXml(ImmutableList.<Content>of(item), baos);
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
