package org.atlasapi.feeds.youview;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.StringPayload;


public class YouViewUploader {

    private static final String INGEST_URL_SUFFIX = "/transaction";
    
    private final TvAnytimeGenerator generator;

    private final String youViewUrl;
    private final SimpleHttpClient httpClient;
    
    private final Logger log = LoggerFactory.getLogger(YouViewUploader.class);
    
    public YouViewUploader(String youViewUrl, TvAnytimeGenerator generator, SimpleHttpClient httpClient) {
        this.youViewUrl = youViewUrl;
        this.generator = generator;
        this.httpClient = httpClient;
    }

    public void upload(Iterable<Content> chunk) throws UnsupportedEncodingException, HttpException {
        String queryUrl = youViewUrl + INGEST_URL_SUFFIX;
        log.info(String.format("Posting YouView output xml to %s", queryUrl));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        generator.generateXml(chunk, baos);
        HttpResponse response = httpClient.post(queryUrl, new StringPayload(baos.toString(Charsets.UTF_8.name())));

        if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
            log.info("Response: " + response.header("Location"));
        } else {
            throw new RuntimeException(String.format("An Http status code of %s was returned when POSTing to YouView. Error message:\n%s", response.statusCode(), response.body()));
        }
    }
}
