package org.atlasapi.feeds.sitemaps.channel4;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.FixedResponseHttpClient;
import com.metabroadcast.common.http.SimpleHttpClient;


public class HttpFetchingC4FlashPlayerVersionSupplierTest {

    private static final String URI = "http://example.org/";
    private final SimpleHttpClient httpClient 
                    = new FixedResponseHttpClient(
                            ImmutableMap.of(URI, "var flashVersion = '12.05';"));
    
    private final HttpFetchingC4FlashPlayerVersionSupplier supplier = new HttpFetchingC4FlashPlayerVersionSupplier(httpClient, URI);
    
    @Test
    public void testVersionNumberIsParsed() {
        assertThat(supplier.get(), is(equalTo("12.05")));
    }
}
