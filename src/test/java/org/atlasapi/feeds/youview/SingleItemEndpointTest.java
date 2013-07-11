package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.LoveFilmGroupInformationHierarchyTest.DummyContentResolver;
import org.atlasapi.feeds.youview.www.YouViewController;
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
    
    private YouViewGenreMapping genreMapping = new YouViewGenreMapping(); 
    private ProgramInformationGenerator progInfoGenerator = new LoveFilmProgramInformationGenerator();
    private GroupInformationGenerator groupInfoGenerator = new LoveFilmGroupInformationGenerator(genreMapping);
    private OnDemandLocationGenerator progLocationGenerator = new LoveFilmOnDemandLocationGenerator();
    private DummyContentResolver contentResolver = new DummyContentResolver();
    
    private TvAnytimeGenerator generator = new DefaultTvAnytimeGenerator(
        progInfoGenerator, 
        groupInfoGenerator, 
        progLocationGenerator, 
        contentResolver,
        false
    );
    
    private YouViewUploader uploader = new YouViewUploader("youviewurl", generator, httpClient);
    private YouViewDeleter deleter = new YouViewDeleter("youviewurl", httpClient);

    private LastUpdatedContentFinder contentFinder = Mockito.mock(LastUpdatedContentFinder.class);
    
    private YouViewController controller = new YouViewController(generator, contentFinder, contentResolver, uploader, deleter);
    
    @Before
    public void setup() throws HttpException, IOException {
        HttpResponse httpResponse = new HttpResponse("", HttpServletResponse.SC_ACCEPTED, "", ImmutableMap.of("Location", "yv location"));
        Mockito.when(httpClient.delete(Mockito.anyString())).thenReturn(httpResponse);
        Mockito.when(response.getOutputStream()).thenReturn(Mockito.mock(ServletOutputStream.class));
    }
    
    @Test
    public void testDeleteCalledByDeletionEndpoint() throws HttpException {
        contentResolver.addContent(createItem("itemUri", "itemASIN"));

        controller.deleteContent(response, "itemUri");

        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/itemUri"));
        Mockito.verify(httpClient, times(2)).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/itemUri"));
    }
    
    @Test
    public void testUploadPerformedByUploadEndpoint() throws IOException, HttpException {
        Item item = createItem("itemUri", "itemASIN");
        contentResolver.addContent(item);

        controller.uploadContent(response, "itemUri");

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
