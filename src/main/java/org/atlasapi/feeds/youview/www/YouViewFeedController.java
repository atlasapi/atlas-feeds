package org.atlasapi.feeds.youview.www;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.time.DateTimeZones;

@Controller
public class YouViewFeedController {

    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);
    
    private final DateTimeFormatter fmt = ISODateTimeFormat.dateHourMinuteSecond().withZone(DateTimeZones.UTC);
    private final TvAnytimeGenerator feedGenerator;
    private final LastUpdatedContentFinder contentFinder;
    private final ContentResolver contentResolver;
    
    public YouViewFeedController(TvAnytimeGenerator feedGenerator, LastUpdatedContentFinder contentFinder, ContentResolver contentResolver) {
        this.feedGenerator = checkNotNull(feedGenerator);
        this.contentFinder = checkNotNull(contentFinder);
        this.contentResolver = checkNotNull(contentResolver);
    }
    
    /**
     * Produces the feed for YouView
     * @param uri -         if present, the endpoint will return the xml generated for that 
     *                      particular item else it will check the lastUpdated parameter.                    
     * @param lastUpdated - if present, the endpoint will return a delta feed of all items 
     *                      updated since lastUpdated, otherwise it will return a full 
     *                      bootstrap feed
     * @throws IOException 
     */
    @RequestMapping(value="/feeds/youview/{publisher}", method = RequestMethod.GET)
    public void generateFeed(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "lastUpdated", required = false) String lastUpdated,
            @RequestParam(value = "uri", required = false) String uri) throws IOException {
        
        Publisher publisher = Publisher.valueOf(publisherStr.trim().toUpperCase());
        if (publisher == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            response.setContentLength(0);
            return;
        }

        Optional<String> since = Optional.fromNullable(lastUpdated);
        Optional<String> possibleUri = Optional.fromNullable(uri);
        feedGenerator.generateXml(getContent(publisher, since, possibleUri), response.getOutputStream());
        
        response.setContentType(MimeType.APPLICATION_XML.toString());
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    private Iterable<Content> getContent(Publisher publisher, Optional<String> since, Optional<String> possibleUri) {
        if (possibleUri.isPresent()) {
            ResolvedContent resolvedContent = contentResolver.findByCanonicalUris(ImmutableList.of(possibleUri.get()));
            Collection<Identified> resolved = resolvedContent.asResolvedMap().values();
            return ImmutableList.of((Content) Iterables.getOnlyElement(resolved));
        } else {
            DateTime start = since.isPresent() ? fmt.parseDateTime(since.get()) : START_OF_TIME;
            return ImmutableList.copyOf(contentFinder.updatedSince(publisher, start));
        }
    }
}
