package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.url.QueryStringParameters;


public class YouViewRemoteClient {
    
    private static final Ordering<Content> HIERARCHICAL_ORDER = new Ordering<Content>() {
        @Override
        public int compare(Content left, Content right) {
            if (left instanceof Item) {
                if (right instanceof Item) {
                    return 0;
                } else {
                    return 1;
                }
            } else if (left instanceof Series) {
                if (right instanceof Item) {
                    return -1;
                } else if (right instanceof Series) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (right instanceof Brand) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    };

    private static final String UPLOAD_URL_SUFFIX = "/transaction";
    private static final String DELETION_URL_SUFFIX = "/fragment";
    private static final String DELETION_TYPE = "id";

    private final Logger log = LoggerFactory.getLogger(YouViewRemoteClient.class);
    
    private final TvAnytimeGenerator generator;
    private final YouViewPerPublisherFactory configurationFactory;
    
    public YouViewRemoteClient(TvAnytimeGenerator generator, YouViewPerPublisherFactory configurationFactory) {
        this.configurationFactory = checkNotNull(configurationFactory);
        this.generator = checkNotNull(generator);
    }
    
    /**
     * <p>Given an Iterable<Content>, generates YouView TVAnytime XML and uploads it to YouView.
     * The configuration used for uploading is based upon the publisher of each piece of content.</p>
     * <p>N.B. The credentials used for upload are chosen from the publisher of the first piece of content.</p> 
     * @param chunk
     * @throws UnsupportedEncodingException
     * @throws HttpException
     */
    public void upload(Iterable<Content> chunk) throws UnsupportedEncodingException, HttpException {
        Content first = Iterables.getFirst(chunk, null);
        if (first == null) {
            log.error("Chunk contained no content");
            return;
        }
        Publisher publisher = first.getPublisher();
        SimpleHttpClient httpClient = configurationFactory.getHttpClient(publisher);
        PublisherConfiguration config = configurationFactory.getConfiguration(publisher);
        
        String queryUrl = config.getYouViewBaseUrl() + UPLOAD_URL_SUFFIX;
        log.trace(String.format("Posting YouView output xml to %s", queryUrl));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        generator.generateXml(chunk, baos);
        HttpResponse response = httpClient.post(queryUrl, new StringPayload(baos.toString(Charsets.UTF_8.name())));

        if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
            log.info("Upload successful. Transaction url: " + response.header("Location"));
        } else {
            throw new RuntimeException(String.format("An Http status code of %s was returned when POSTing to YouView. Error message:\n%s", response.statusCode(), response.body()));
        }
    }
    
    public static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }
    
    public boolean sendDeleteFor(Content content) {
        if (content instanceof Item) {
            return sendDelete((Item) content);
        } else if (content instanceof Series) {
            return sendDelete((Series) content);
        } else if (content instanceof Brand) {
            return sendDelete((Brand) content);
        } else {
            throw new IllegalArgumentException(String.format("content type %s not recognised. Uri: %s", content.getClass(), content.getCanonicalUri()));
        }
    }
    
    // TODO does this need two deletes for the item crid? surely it only needs deletes for crid, version-crid and imi?
    private boolean sendDelete(Item item) {
        Publisher publisher = item.getPublisher();
        IdParser idParser = configurationFactory.getIdParser(publisher);
        PublisherConfiguration config = configurationFactory.getConfiguration(publisher);
        SimpleHttpClient httpClient = configurationFactory.getHttpClient(publisher);
        
        return sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createCrid(config.getCridPrefix(), item))
//                && sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createCrid(config.getCridPrefix(), item))
                && sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createVersionCrid(config.getCridPrefix(), item))
                && sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createImi(config.getImiPrefix(), item));
    }
    
    private boolean sendDelete(Series series) {
        Publisher publisher = series.getPublisher();
        IdParser idParser = configurationFactory.getIdParser(publisher);
        PublisherConfiguration config = configurationFactory.getConfiguration(publisher);
        SimpleHttpClient httpClient = configurationFactory.getHttpClient(publisher);
        
        return sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createCrid(config.getCridPrefix(), series));
    }
    
    private boolean sendDelete(Brand brand) {
        Publisher publisher = brand.getPublisher();
        IdParser idParser = configurationFactory.getIdParser(publisher);
        PublisherConfiguration config = configurationFactory.getConfiguration(publisher);
        SimpleHttpClient httpClient = configurationFactory.getHttpClient(publisher);
        
        return sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createCrid(config.getCridPrefix(), brand));
    }
    
    private boolean sendDelete(SimpleHttpClient httpClient, String baseUrl, String id) {
        QueryStringParameters qsp = new QueryStringParameters();
        qsp.add(DELETION_TYPE, id);
        String queryUrl = baseUrl + DELETION_URL_SUFFIX;
        log.info(String.format("Deleting YouView content with %s %s at %s", DELETION_TYPE, id, queryUrl));
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
