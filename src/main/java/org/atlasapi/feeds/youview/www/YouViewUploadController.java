package org.atlasapi.feeds.youview.www;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.youview.YouViewRemoteClient;
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

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.time.DateTimeZones;

@Controller
public class YouViewUploadController {

    private static final DateTime START_OF_TIME = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.UTC);
    
    private final DateTimeFormatter fmt = ISODateTimeFormat.dateHourMinuteSecond().withZone(DateTimeZones.UTC);
    private final LastUpdatedContentFinder contentFinder;
    private final ContentResolver contentResolver;
    private final YouViewRemoteClient remoteClient;
    
    public YouViewUploadController(LastUpdatedContentFinder contentFinder, ContentResolver contentResolver, YouViewRemoteClient remoteClient) {
        this.contentFinder = checkNotNull(contentFinder);
        this.contentResolver = checkNotNull(contentResolver);
        this.remoteClient = checkNotNull(remoteClient);
    }
        
    /**
     * Uploads the XML for a particular item to YouView
     * @param uri -         the endpoint will upload the xml generated for the particular 
     *                      item to youview.                    
     * @throws IOException
     * @throws HttpException 
     */
    @RequestMapping(value="/feeds/youview/{publisher}/upload", method = RequestMethod.POST)
    public void uploadContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException, HttpException {
        
        Publisher publisher = Publisher.valueOf(publisherStr.trim().toUpperCase());
        if (publisher == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            response.setContentLength(0);
            return;
        }

        Optional<String> possibleUri = Optional.fromNullable(uri);
        Iterable<Content> content = getContent(publisher, Optional.<String>absent(), possibleUri);

        remoteClient.upload(content);

        response.setStatus(HttpServletResponse.SC_OK);
        String message = "Upload for " + uri + " sent sucessfully";
        response.getOutputStream().write(message.getBytes(Charsets.UTF_8));
    }
    
    /**
     * Sends deletes for the XML elements representing a particular item to YouView
     * @param uri -         the endpoint will send the delete calls to remove the XML generated 
     *                      for the particular item.                    
     * @throws IOException 
     */    
    @RequestMapping(value="/feeds/youview/{publisher}/delete", method = RequestMethod.POST)
    public void deleteContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {

        Publisher publisher = Publisher.valueOf(publisherStr.trim().toUpperCase());
        if (publisher == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            response.setContentLength(0);
            return;
        }

        Content toBeDeleted = Iterables.getOnlyElement(getContent(publisher, Optional.<String>absent(), Optional.fromNullable(uri)));
        
        remoteClient.sendDeleteFor(toBeDeleted);

        response.setStatus(HttpServletResponse.SC_OK);
        String message = "Delete for " + uri + " sent sucessfully";
        response.getOutputStream().write(message.getBytes(Charsets.UTF_8));
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
