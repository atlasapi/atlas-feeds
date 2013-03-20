package org.atlasapi.feeds.youview;

import java.io.ByteArrayOutputStream;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.utils.UpdateProgress;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.DateTimeZones;

public class YouViewUploader extends ScheduledTask {

    // TODO if more publishers are required, make this a list & a parameter of the class
    private static final Publisher PUBLISHER = Publisher.LOVEFILM;
    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);

    private final int chunkSize;
    private final String youViewUrl;
    private final LastUpdatedContentFinder contentFinder;
    private final boolean isBootstrap;
    private final TvAnytimeGenerator generator;
    private final SimpleHttpClient httpClient;
    
    private final Logger log = LoggerFactory.getLogger(YouViewUploader.class);
    private final YouViewLastUpdatedStore store;
    
    public YouViewUploader(String youViewUrl, LastUpdatedContentFinder contentFinder, TvAnytimeGenerator generator, UsernameAndPassword credentials, YouViewLastUpdatedStore store, int chunkSize, boolean isBootstrap) {
        this.youViewUrl = youViewUrl;
        this.contentFinder = contentFinder;
        this.store = store;
        this.isBootstrap = isBootstrap;
        this.generator = generator;
        this.chunkSize = chunkSize;
        this.httpClient = new SimpleHttpClientBuilder()
            .withHeader("Content-Type", "text/xml")
            .withSocketTimeout(1, TimeUnit.MINUTES)
            .withPreemptiveBasicAuth(credentials)
            .build();
    }
    
    @Override
    protected void runTask() {
        
        Iterable<Content> allContent;
        DateTime lastUpdated;
        
        if (isBootstrap) {
            allContent = getContentSinceDate(Optional.<DateTime>absent());
        } else {
            try {
                lastUpdated = store.getLastUpdated();
                allContent = getContentSinceDate(Optional.of(lastUpdated));
            } catch(NoSuchElementException e) {
                log.error("The bootstrap has not successfully run. Please run the bootstrap upload and ensure that it succeeds before running the delta upload.");
                Throwables.propagate(e);
                // will never reach this
                return;
            }
        }
        
        lastUpdated = new DateTime();
        
        UpdateProgress progress = UpdateProgress.START;
        
        for (Iterable<Content> contents : Iterables.partition(allContent, chunkSize)) {
            if (!shouldContinue()) {
                break;
            }
           
            try {
                log.info(String.format("Posting YouView output xml to %s", youViewUrl));
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                generator.generateXml(contents, baos, isBootstrap);
                HttpResponse response = httpClient.post(youViewUrl, new StringPayload(baos.toString(Charsets.UTF_8.name())));
                
                if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                    log.info("Response: " + response.header("Location"));
                } else {
                    throw new RuntimeException(String.format("An Http status code of %s was returned when POSTing to YouView. Error message:\n%s", response.statusCode(), response.body()));
                }
                progress = progress.reduce(UpdateProgress.SUCCESS);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                progress = progress.reduce(UpdateProgress.FAILURE);
            }
            reportStatus(progress.toString());
        }
        store.setLastUpdated(lastUpdated);
    }
    
    private Iterable<Content> getContentSinceDate(Optional<DateTime> since) {
        DateTime start = since.isPresent() ? since.get() : START_OF_TIME;
        return ImmutableList.copyOf(contentFinder.updatedSince(PUBLISHER, start));
    }
}
