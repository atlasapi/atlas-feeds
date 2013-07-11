package org.atlasapi.feeds.youview;

import static org.mockito.Mockito.times;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.url.UrlEncoding;

public class DeletionOrderTest {
    
    private HttpResponse response;
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    
    private final YouViewDeleter deleter = new YouViewDeleter("youviewurl", httpClient);
    
    public DeletionOrderTest() throws HttpException {
        response = new HttpResponse("", HttpServletResponse.SC_ACCEPTED, "", ImmutableMap.of("Location", "yv location"));
        Mockito.when(httpClient.delete(Mockito.anyString())).thenReturn(response); 
    }
    
    @Test
    public void testItemDeletion() throws HttpException {
        List<Content> deletes = ImmutableList.<Content>of(createItem("http://lovefilm.com/episodes/1234", "episode1234"), createItem("http://lovefilm.com/episodes/5678", "episode5678")); 
        // monitor http calls
        deleter.sendDeletes(deletes);

        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/1234"));
        Mockito.verify(httpClient, times(2)).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/1234"));
        
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/5678"));
        Mockito.verify(httpClient, times(2)).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/5678"));
    }
    
    @Test
    public void testOrderedContentDeletion() throws HttpException {
        List<Content> deletes = ImmutableList.<Content>of(createBrand("http://lovefilm.com/shows/1234", "brand1234"), createSeries("http://lovefilm.com/seasons/2345", "series2345"), createItem("http://lovefilm.com/episodes/3456", "episode3456"));
        // do all possible permutations of this iterable 
        // monitor http calls
        deleter.sendDeletes(deletes);

        // Item
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/3456"));
        Mockito.verify(httpClient, times(2)).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/3456"));
        // Series
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/2345"));
        // Brand
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
