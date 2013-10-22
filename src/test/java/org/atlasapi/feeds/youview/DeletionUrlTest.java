package org.atlasapi.feeds.youview;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.url.UrlEncoding;

public class DeletionUrlTest {
    
    private HttpResponse response;
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private TvAnytimeGenerator generator = Mockito.mock(TvAnytimeGenerator.class);
    private YouViewPerPublisherFactory configurationFactory = YouViewPerPublisherFactory.builder()
            .withPublisher(Publisher.LOVEFILM, new LoveFilmPublisherConfiguration("youviewurl"), new LoveFilmIdParser(), Mockito.mock(GenreMap.class), httpClient)
            .build();
    
    private final YouViewRemoteClient youViewClient = new YouViewRemoteClient(generator, configurationFactory);
    
    public DeletionUrlTest() throws HttpException {
        response = new HttpResponse("", HttpServletResponse.SC_ACCEPTED, "", ImmutableMap.of("Location", "yv location"));
        Mockito.when(httpClient.delete(Mockito.anyString())).thenReturn(response); 
    }
    
    @Test
    public void testItemDeletion() throws HttpException {
        Item item = createItem("http://lovefilm.com/episodes/1234", "episode1234"); 
        youViewClient.sendDeleteFor(item);

        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/1234"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/1234_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/1234"));
    }
    
    @Test
    public void testSeriesDeletion() throws HttpException {
        Series series = createSeries("http://lovefilm.com/seasons/1234", "series1234"); 
        youViewClient.sendDeleteFor(series);

        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/1234"));
    }
    
    @Test
    public void testBrandDeletion() throws HttpException {
        Brand brand = createBrand("http://lovefilm.com/shows/1234", "brand1234"); 
        youViewClient.sendDeleteFor(brand);

        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/1234"));
    }

    private Brand createBrand(String uri, String asin) {
        Brand brand= new Brand();
        setCommonFields(brand, uri, asin);
        return brand;
    }

    private void setCommonFields(Content content, String uri, String asin) {
        content.setPublisher(Publisher.LOVEFILM);
        content.setCanonicalUri(uri);
        content.addAlias(new Alias("gb:amazon:asin", asin));
        content.setLastUpdated(new DateTime());
    }

    private Series createSeries(String uri, String asin) {
        Series series = new Series();
        setCommonFields(series, uri, asin);
        return series;
    }

    private Item createItem(String uri, String asin) {
        Item item = new Item();
        setCommonFields(item, uri, asin);
        return item;
    }
}
