package org.atlasapi.feeds.youview;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.internal.ImmutableList;
import com.google.inject.internal.ImmutableMap;
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
        List<Content> deletes = ImmutableList.<Content>of(createItem("http://lovefilm.com/episodes/1234"), createItem("http://lovefilm.com/episodes/5678")); 
        // monitor http calls
        deleter.sendDeletes(deletes);

        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/t1234_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/1234_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/1234"));
        
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/t5678_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/5678_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/5678"));
    }
    
    @Test
    public void testOrderedContentDeletion() throws HttpException {
        List<Content> deletes = ImmutableList.<Content>of(createBrand("http://lovefilm.com/shows/1234"), createSeries("http://lovefilm.com/seasons/2345"), createItem("http://lovefilm.com/episodes/3456"));
        // do all possible permutations of this iterable 
        // monitor http calls
        deleter.sendDeletes(deletes);

        // Item
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("imi:lovefilm.com/t3456_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/3456_version"));
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/3456"));
        // Series
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/2345"));
        // Brand
        Mockito.verify(httpClient).delete("youviewurl/fragment?id=" + UrlEncoding.encode("crid://lovefilm.com/product/1234"));
    }

    private Brand createBrand(String uri) {
        Brand brand= new Brand();
        setCommonFields(brand, uri);
        return brand;
    }

    private void setCommonFields(Content content, String uri) {
        content.setPublisher(Publisher.LOVEFILM);
        content.setCanonicalUri(uri);
        content.setLastUpdated(new DateTime());
    }

    private Series createSeries(String uri) {
        Series series = new Series();
        setCommonFields(series, uri);
        return series;
    }

    private Item createItem(String uri) {
        Item item = new Item();
        setCommonFields(item, uri);
        return item;
    }
}
