package org.atlasapi.feeds.youview;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Ordering;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.url.QueryStringParameters;

public class YouViewDeleter {

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
    
    private static final String DELETE_URL_SUFFIX = "/fragment";
    private static final String DELETE_TYPE = "id";
    
    private final String youViewUrl;
    private final SimpleHttpClient httpClient;
    
    private final Logger log = LoggerFactory.getLogger(YouViewDeleter.class);
    
    public YouViewDeleter(String youViewUrl, SimpleHttpClient httpClient) {
        this.youViewUrl = youViewUrl;
        this.httpClient = httpClient;
    }

    // TODO refactor progress count out
    public int sendDeletes(Iterable<Content> toBeDeleted) {
        List<Content> orderedDeletes = REVERSE_HIERARCHICAL_ORDER.sortedCopy(toBeDeleted);
        int successes = 0;
        for (Content deleted : orderedDeletes) {
            try {
                if (deleted instanceof Item) {
                    if (sendDelete(LoveFilmOnDemandLocationGenerator.createImi((Item) deleted))) {
                        successes++;
                    }
                    if (sendDelete(LoveFilmProgramInformationGenerator.createCrid((Item) deleted))) {
                        successes++;
                    }
                    if (sendDelete(LoveFilmGroupInformationGenerator.createCrid(deleted))) {
                        successes++;
                    }
                } else if (deleted instanceof Series) {
                    if (sendDelete(LoveFilmGroupInformationGenerator.createCrid(deleted))) {
                        successes++;
                    }
                } else if (deleted instanceof Brand) {
                    if (sendDelete(LoveFilmGroupInformationGenerator.createCrid(deleted))) {
                        successes++;
                    }
                }
            } catch (RuntimeException e) {
                log.error("Failed to delete content", e); 
            }
        }
        return successes;
    }
    
    

    private boolean sendDelete(String id) {
        QueryStringParameters qsp = new QueryStringParameters();
        qsp.add(DELETE_TYPE, id);
        String queryUrl = youViewUrl + DELETE_URL_SUFFIX;
        log.info(String.format("Deleting YouView content with %s %s at %s", DELETE_TYPE, id, queryUrl));
        try {
            HttpResponse response = httpClient.delete(queryUrl + "?" + qsp.toString());
            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                log.info("Response: " + response.header("Location"));
                return true;
            } else {
                log.error(String.format("An Http status code of %s was returned when POSTing to YouView. Error message:\n%s", response.statusCode(), response.body()));
                return false;
            }
        } catch (HttpException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
