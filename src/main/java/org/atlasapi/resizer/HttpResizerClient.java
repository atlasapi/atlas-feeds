package org.atlasapi.resizer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.api.client.util.ExponentialBackOff;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpResizerClient implements ResizerClient {

    public static final String RESIZER_BASE_URL = "http://users-images-atlas.metabroadcast.com/";

    private final HttpRequestFactory requestFactory;
    private final LoadingCache<String, ImageSize> cache = CacheBuilder.newBuilder()
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .build(new CacheLoader<String, ImageSize>() {
                @Override
                public ImageSize load(String key) throws Exception {
                    return doGetImageDimensions(key);
                }
            });

    public HttpResizerClient(HttpTransport transport) {
        requestFactory = checkNotNull(transport.createRequestFactory());
    }

    @Override
    public ImageSize getImageDimensions(String url)  {
        try {
            return cache.get(url);
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    private ImageSize doGetImageDimensions(String url) throws IOException {
        if (Strings.isNullOrEmpty(url) || !url.startsWith(RESIZER_BASE_URL)) {
            throw new IllegalArgumentException(String.format("%s is not a Resizer URL", url));
        }
        HttpRequest httpRequest = requestFactory.buildGetRequest(new GenericUrl(url));

        httpRequest.setUnsuccessfulResponseHandler(
                new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff())
        );

        HttpResponse response = httpRequest.execute();
        BufferedImage image = ImageIO.read(response.getContent());
        return new ImageSize(image.getHeight(), image.getWidth());
    }

}
