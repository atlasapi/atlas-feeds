package org.atlasapi.feeds.youview.www;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.ApplicationSources;
import org.atlasapi.application.ApplicationSourcesFetcher;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.media.content.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.time.DateTimeZones;

@Controller
public class YouViewController {

    // TODO if more publishers are required, make this a list & a parameter of the class
    private static final Publisher PUBLISHER = Publisher.LOVEFILM;
    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);
    
    private final DateTimeFormatter fmt = ISODateTimeFormat.dateHourMinuteSecond().withZone(DateTimeZones.UTC);
    private final TvAnytimeGenerator feedGenerator;
    private final LastUpdatedContentFinder contentFinder;
    private final ApplicationSourcesFetcher sourcesFetcher;
    
    public YouViewController(ApplicationSourcesFetcher sourcesFetcher, TvAnytimeGenerator feedGenerator, LastUpdatedContentFinder contentFinder) {
        this.sourcesFetcher = sourcesFetcher;
        this.feedGenerator = feedGenerator;
        this.contentFinder = contentFinder;
    }
    
    /**
     * Produces the feed for YouView
     * @param response
     * @param lastUpdated - if present, the endpoint will return a delta feed of all items 
     *                      updated since lastUpdated, otherwise it will return a full 
     *                      bootstrap feed
     * @throws IOException 
     */
    @RequestMapping("/feeds/youview/lovefilm")
    public void generateFeed(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "lastUpdated", required = false) String lastUpdated) throws IOException {
        try {
            final ApplicationSources appConfig = appSources(request);
            if (!appConfig.isReadEnabled(PUBLISHER)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentLength(0);
                return;
            }
            response.setContentType(MimeType.APPLICATION_ATOM_XML.toString());
            response.setStatus(HttpServletResponse.SC_OK);
            
            Optional<String> since = Optional.fromNullable(lastUpdated);
            feedGenerator.generateXml(getContentSinceDate(since), response.getOutputStream());
            
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
        }
    }
    
    private Iterable<Content> getContentSinceDate(Optional<String> since) {
        
        DateTime start = since.isPresent() ? fmt.parseDateTime(since.get()) : START_OF_TIME;
        return ImmutableList.copyOf(contentFinder.updatedSince(PUBLISHER, start));
    }
    
    private ApplicationSources appSources(HttpServletRequest request) {
        Optional<ApplicationSources> config = sourcesFetcher.sourcesFor(request);
        return config.isPresent() ? config.get() : ApplicationSources.DEFAULT_SOURCES;
    }
}
