package org.atlasapi.feeds.youview.www;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.youview.upload.YouViewClient;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.http.HttpException;

@Controller
public class YouViewUploadController {

    private final ContentResolver contentResolver;
    private final YouViewClient remoteClient;
    
    public YouViewUploadController(ContentResolver contentResolver, YouViewClient remoteClient) {
        this.contentResolver = checkNotNull(contentResolver);
        this.remoteClient = checkNotNull(remoteClient);
    }
        
    /**
     * Uploads the XML for a particular piece of content to YouView
     * @param uri the canonical uri for the item to upload
     * @throws IOException
     * @throws HttpException 
     */
    @RequestMapping(value="/feeds/youview/{publisher}/upload", method = RequestMethod.POST)
    public void uploadContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException, HttpException {
        
        Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
        if (!publisher.isPresent()) {
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }
        Optional<Content> toBeUploaded = getContent(publisher.get(), uri);
        if (!toBeUploaded.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "content does not exist");
            return;
        }
        remoteClient.upload(toBeUploaded.get());
        
        sendOkResponse(response, "Upload for " + uri + " sent sucessfully");
    }
    
    /**
     * Sends deletes for the XML elements representing a particular piece of content to YouView
     * @param uri the canonical uri for the item to delete
     * @throws IOException 
     */    
    @RequestMapping(value="/feeds/youview/{publisher}/delete", method = RequestMethod.POST)
    public void deleteContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {

        Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
        if (!publisher.isPresent()) {
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }
        Optional<Content> toBeDeleted = getContent(publisher.get(), uri);
        if (!toBeDeleted.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "content does not exist");
            return;
        }
        remoteClient.sendDeleteFor(toBeDeleted.get());

        sendOkResponse(response, "Delete for " + uri + " sent sucessfully");
    }
    
    /**
    * Revokes a particular piece of content from the YouView system.
    * @param uri the canonical uri for the item to revoke
    * @throws IOException 
    */ 
    @RequestMapping(value="/feeds/youview/{publisher}/revoke", method = RequestMethod.POST)
    public void revokeContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {

        Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
        if (!publisher.isPresent()) {
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }

        Optional<Content> toBeRevoked = getContent(publisher.get(), uri);
        if (!toBeRevoked.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "content does not exist");
            return;
        }
        remoteClient.revoke(toBeRevoked.get());

        sendOkResponse(response, "Revoke for " + uri + " sent sucessfully");
    }
    
    /**
     * Unrevokes a particular piece of content from the YouView system.
     * @param uri the canonical uri for the item to unrevoke
     * @throws IOException 
     */ 
    @RequestMapping(value="/feeds/youview/{publisher}/unrevoke", method = RequestMethod.POST)
    public void unrevokeContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {
        
        Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
        if (!publisher.isPresent()) {
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }
        Optional<Content> toBeUnrevoked = getContent(publisher.get(), uri);
        if (!toBeUnrevoked.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "content does not exist");
            return;
        }
        remoteClient.unrevoke(toBeUnrevoked.get());

        sendOkResponse(response, "Unrevoke for " + uri + " sent sucessfully");
    }

    private Optional<Publisher> findPublisher(String publisherStr) {
        for (Publisher publisher : Publisher.all()) {
            if (publisher.name().equals(publisherStr)) {
                return Optional.of(publisher);
            }
        }
        return Optional.absent();
    }

    private void sendError(HttpServletResponse response, int responseCode, String message) throws IOException {
        response.sendError(responseCode, message);
        response.setContentLength(0);
    }
    
    private Optional<Content> getContent(Publisher publisher, String contentUri) {
        ResolvedContent resolvedContent = contentResolver.findByCanonicalUris(ImmutableList.of(contentUri));
        return Optional.fromNullable((Content) resolvedContent.getFirstValue().valueOrNull());
    }
    
    private void sendOkResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(message.getBytes(Charsets.UTF_8));
    }
}
