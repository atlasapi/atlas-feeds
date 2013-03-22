package org.atlasapi.feeds.youview;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.utils.UpdateProgress;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.inject.internal.Lists;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.url.QueryStringParameters;

public class YouViewUploader extends ScheduledTask {

    private static final String INGEST_URL_SUFFIX = "/transaction";
    private static final String DELETE_URL_SUFFIX = "/fragment";
    private static final String DELETE_TYPE = "id";
    // TODO if more publishers are required, make this a list & a parameter of the class
    private static final Publisher PUBLISHER = Publisher.LOVEFILM;
    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);
    public static final Ordering<Content> REVERSE_HIERARCHICAL_ORDER = new Ordering<Content>() {
        @Override
        public int compare(Content left, Content right) {
            if (left instanceof Item) {
                if (right instanceof Item) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (left instanceof Series) {
                if (right instanceof Item) {
                    return 1;
                } else if (right instanceof Series) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                if (right instanceof Brand) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }
    };

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
        
        DateTime lastUpdated;
        
        if (isBootstrap) {
            lastUpdated = new DateTime();
            Iterable<Content> allContent = Iterables.filter(getContentSinceDate(Optional.<DateTime>absent()), new Predicate<Content>() {
                @Override
                public boolean apply(Content input) {
                    return input.isActivelyPublished();
                }
            });
            UpdateProgress progress = UpdateProgress.START;
            progress = uploadChunkedContent(allContent, progress);
            store.setLastUpdated(lastUpdated);
        } else {
            try {
                lastUpdated = store.getLastUpdated();
                Iterable<Content> updatedContent = getContentSinceDate(Optional.of(lastUpdated));
                lastUpdated = new DateTime();    
                
                List<Content> deleted = Lists.newArrayList();
                List<Content> notDeleted = Lists.newArrayList();
                for (Content updated : updatedContent) {
                    if (updated.isActivelyPublished()) {
                        notDeleted.add(updated);
                    } else {
                        deleted.add(updated);
                    }
                }
                
                UpdateProgress progress = UpdateProgress.START;
                progress = uploadChunkedContent(notDeleted, progress);
                
                sendDeletes(deleted, progress);
                
                store.setLastUpdated(lastUpdated);
            } catch(NoSuchElementException e) {
                log.error("The bootstrap has not successfully run. Please run the bootstrap upload and ensure that it succeeds before running the delta upload.");
                Throwables.propagate(e);
                // will never reach this
                return;
            }
        }
        
    }

    // TODO better update progress?
    private void sendDeletes(Iterable<Content> deleted, UpdateProgress progress) {
        List<Content> orderedContent = REVERSE_HIERARCHICAL_ORDER.sortedCopy(deleted);
        
        for (Content content : orderedContent) {
            if (content instanceof Item) {
                progress = progress.reduce(sendDelete(LoveFilmOnDemandLocationGenerator.createImi((Item) content)));
                progress = progress.reduce(sendDelete(LoveFilmProgramInformationGenerator.createCrid((Item) content)));
                progress = progress.reduce(sendDelete(LoveFilmGroupInformationGenerator.createCrid(content)));
            } else if (content instanceof Series) {
                progress = progress.reduce(sendDelete(LoveFilmGroupInformationGenerator.createCrid(content)));
            } else if (content instanceof Brand) {
                progress = progress.reduce(sendDelete(LoveFilmGroupInformationGenerator.createCrid(content)));
            }
            
            reportStatus("Deletes: " + progress.toString());
        }
    }

    private UpdateProgress sendDelete(String id) {
        QueryStringParameters qsp = new QueryStringParameters();
        qsp.add(DELETE_TYPE, id);
        try {
            String queryUrl = youViewUrl + DELETE_URL_SUFFIX;
            log.info(String.format("Deleting YouView content with %s %s at %s", DELETE_TYPE, id, queryUrl));
            HttpResponse response = httpClient.delete(queryUrl + "?" + qsp.toString());
            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                log.info("Response: " + response.header("Location"));
            } else {
                throw new RuntimeException(String.format("An Http status code of %s was returned when POSTing to YouView. Error message:\n%s", response.statusCode(), response.body()));
            }
            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return UpdateProgress.FAILURE;
        }
    }

    private UpdateProgress uploadChunkedContent(Iterable<Content> allContent,
            UpdateProgress progress) {
        for (Iterable<Content> contents : Iterables.partition(allContent, chunkSize)) {
            if (!shouldContinue()) {
                break;
            }
           
            try {
                String queryUrl = youViewUrl + INGEST_URL_SUFFIX;
                log.info(String.format("Posting YouView output xml to %s", queryUrl));
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                generator.generateXml(contents, baos);
                HttpResponse response = httpClient.post(queryUrl, new StringPayload(baos.toString(Charsets.UTF_8.name())));
                
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
            reportStatus("Uploads: " + progress.toString());
        }
        return progress;
    }
    
    private Iterable<Content> getContentSinceDate(Optional<DateTime> since) {
        DateTime start = since.isPresent() ? since.get() : START_OF_TIME;
        return ImmutableList.copyOf(contentFinder.updatedSince(PUBLISHER, start));
    }
}
