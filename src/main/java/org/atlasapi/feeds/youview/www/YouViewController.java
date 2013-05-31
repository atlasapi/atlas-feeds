package org.atlasapi.feeds.youview.www;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.YouViewDeleter;
import org.atlasapi.feeds.youview.YouViewUploader;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.time.DateTimeZones;

@Controller
public class YouViewController {

    // TODO if more publishers are required, make this a list & a parameter of the class
    private static final Publisher PUBLISHER = Publisher.LOVEFILM;
    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);
    
    private final DateTimeFormatter fmt = ISODateTimeFormat.dateHourMinuteSecond().withZone(DateTimeZones.UTC);
    private final Logger log = LoggerFactory.getLogger(YouViewController.class);
    private final TvAnytimeGenerator feedGenerator;
    private final LastUpdatedContentFinder contentFinder;
    private final ContentResolver contentResolver;
    private final YouViewUploader uploader;
    private final YouViewDeleter deleter;
    
    public YouViewController(TvAnytimeGenerator feedGenerator, LastUpdatedContentFinder contentFinder, ContentResolver contentResolver, YouViewUploader uploader, YouViewDeleter deleter) {
        this.feedGenerator = feedGenerator;
        this.contentFinder = contentFinder;
        this.contentResolver = contentResolver;
        this.uploader = uploader;
        this.deleter = deleter;
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
    @RequestMapping("/feeds/youview/lovefilm")
    public void generateFeed(HttpServletResponse response,
            @RequestParam(value = "lastUpdated", required = false) String lastUpdated,
            @RequestParam(value = "uri", required = false) String uri) throws IOException {
        try {
            response.setContentType(MimeType.APPLICATION_XML.toString());
            response.setStatus(HttpServletResponse.SC_OK);
            
            Optional<String> since = Optional.fromNullable(lastUpdated);
            Optional<String> possibleUri = Optional.fromNullable(uri);
            feedGenerator.generateXml(getContent(since, possibleUri), response.getOutputStream());
            
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
        }
    }
    
    /**
     * Uploads the XML for a particular item to YouView
     * @param uri -         the endpoint will upload the xml generated for the particular 
     *                      item to youview.                    
     * @throws RuntimeException
     * @throws HttpException 
     */
    @RequestMapping("/feeds/youview/lovefilm/upload")
    public void uploadContent(HttpServletResponse response,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {
        try {
            Optional<String> possibleUri = Optional.fromNullable(uri);
            Iterable<Content> content = getContent(Optional.<String>absent(), possibleUri);
            
            uploader.upload(content);
            
            response.setStatus(HttpServletResponse.SC_OK);
            String message = "Upload for " + uri + " sent sucessfully";
            response.getOutputStream().write(message.getBytes(Charsets.UTF_8));
            
        } catch (IOException e) {
            log.error("Error writing response: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
        } catch (RuntimeException e) {
            log.error("Error uploading to YouView: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
        } catch (HttpException e) {
            log.error("HttpException thrown when POSTing to youview: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
        }
    }
    
    /**
     * Sends deletes for the XML elements representing a particular item to YouView
     * @param uri -         the endpoint will send the delete calls to remove the XML generated 
     *                      for the particular item.                    
     */    
    @RequestMapping("/feeds/youview/lovefilm/delete")
    public void deleteContent(HttpServletResponse response,
            @RequestParam(value = "uri", required = true) String uri) {
        try {
            Optional<String> possibleUri = Optional.fromNullable(uri);
            Iterable<Content> content = getContent(Optional.<String>absent(), possibleUri);

            deleter.sendDeletes(content);
            
            response.setStatus(HttpServletResponse.SC_OK);
            String message = "Delete for " + uri + " sent sucessfully";
            response.getOutputStream().write(message.getBytes(Charsets.UTF_8));
            
        } catch (IOException e) {
            log.error("Error writing response: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
        }
    }
    
    private Iterable<Content> getContent(Optional<String> since, Optional<String> possibleUri) {
        if (possibleUri.isPresent()) {
            ResolvedContent resolvedContent = contentResolver.findByCanonicalUris(ImmutableList.of(possibleUri.get()));
            Collection<Identified> resolved = resolvedContent.asResolvedMap().values();
            return ImmutableList.of((Content) Iterables.getOnlyElement(resolved));
        } else {
            DateTime start = since.isPresent() ? fmt.parseDateTime(since.get()) : START_OF_TIME;
            return ImmutableList.copyOf(contentFinder.updatedSince(PUBLISHER, start));
        }
    }
}
