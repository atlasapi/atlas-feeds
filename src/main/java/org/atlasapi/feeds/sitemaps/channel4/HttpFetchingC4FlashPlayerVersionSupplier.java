package org.atlasapi.feeds.sitemaps.channel4;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;


public class HttpFetchingC4FlashPlayerVersionSupplier implements Supplier<String> {

    private static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("^.*'(.*)'.*");
    
    private final String uri;
    private final SimpleHttpClient httpClient;
    
    public HttpFetchingC4FlashPlayerVersionSupplier(SimpleHttpClient httpClient, String uri) {
        this.httpClient = checkNotNull(httpClient);
        this.uri = checkNotNull(uri);
    }
    
    @Override
    public String get() {
        try {
            return httpClient.get(SimpleHttpRequest.httpRequestFrom(uri, new HttpResponseTransformer<String>() {

                @Override
                public String transform(HttpResponsePrologue prologue, InputStream body)
                        throws HttpException, Exception {
                    InputStreamReader reader = new InputStreamReader(body);
                    String content = CharStreams.toString(reader);
                    Matcher matcher = VERSION_NUMBER_PATTERN.matcher(content);
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                    throw new RuntimeException("Failed to parse return value of " + content);
                }
            }));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

}
