package org.atlasapi.feeds.sitemaps.channel4;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import com.google.common.base.Supplier;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.FixedResponseHttpClient;
import com.metabroadcast.common.http.SimpleHttpClient;
import org.junit.rules.ExpectedException;


public class HttpFetchingC4FlashPlayerVersionSupplierTest {
    private static final String URI = "http://example.org/";

    Supplier<String> createSupplierForResponse(String testResponse) {
        SimpleHttpClient httpClient
                = new FixedResponseHttpClient(
                ImmutableMap.of(URI, testResponse));

        return new HttpFetchingC4FlashPlayerVersionSupplier(httpClient, URI);
    }

    @Test
    public void testVersionNumberIsParsed() {
        Supplier<String> supplier = createSupplierForResponse("var flashVersion = '12.36.3';\nvar useYospace = true;");
        assertThat(supplier.get(), is(equalTo("12.36.3")));
    }

    @Test(expected=RuntimeException.class)
    public void testMissingVersionNumber() {
        Supplier<String> supplier = createSupplierForResponse("var flashVersion = '';\nvar useYospace = true;");
        supplier.get();
    }

    @Test
    public void testAssignmentPadding() {
        Supplier<String> supplier;

        supplier = createSupplierForResponse("flashVersion= '12.36.3';\n");
        assertThat(supplier.get(), is(equalTo("12.36.3")));

        supplier = createSupplierForResponse("flashVersion  \t='12.36.4';\n");
        assertThat(supplier.get(), is(equalTo("12.36.4")));

        supplier = createSupplierForResponse("flashVersion     ='12.3';\n");
        assertThat(supplier.get(), is(equalTo("12.3")));
    }

    @Test(expected=RuntimeException.class)
    public void testNonNumeric() {
        Supplier<String> supplier = createSupplierForResponse("flashVersion= 'abc.abc.abc';\n");
        supplier.get();
    }

    @Test
    public void testMultiPartVersion() {
        Supplier<String> supplier;

        supplier = createSupplierForResponse("flashVersion = '13';\n");
        assertThat(supplier.get(), is(equalTo("13")));

        supplier = createSupplierForResponse("flashVersion = '12.36';\n");
        assertThat(supplier.get(), is(equalTo("12.36")));

        supplier = createSupplierForResponse("flashVersion = '12.3.4';\n");
        assertThat(supplier.get(), is(equalTo("12.3.4")));

        supplier = createSupplierForResponse("flashVersion = '12.3.6.7';\n");
        assertThat(supplier.get(), is(equalTo("12.3.6.7")));
    }


}
