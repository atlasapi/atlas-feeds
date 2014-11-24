package org.atlasapi.feeds.youview.www;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.youview.upload.YouViewClient;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }
        
        Iterable<Content> content = getContent(publisher, uri);

        remoteClient.upload(Iterables.getOnlyElement(content));
        
        sendOkResponse(response, "Upload for " + uri + " sent sucessfully");
    }

    private void sendError(HttpServletResponse response, int responseCode, String message) throws IOException {
        response.sendError(responseCode, message);
        response.setContentLength(0);
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
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }

        Content toBeDeleted = Iterables.getOnlyElement(getContent(publisher, uri));
        remoteClient.sendDeleteFor(toBeDeleted);

        sendOkResponse(response, "Delete for " + uri + " sent sucessfully");
    }
    
    @RequestMapping(value="/feeds/youview/{publisher}/revoke", method = RequestMethod.POST)
    public void revokeContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {

        Publisher publisher = Publisher.valueOf(publisherStr.trim().toUpperCase());
        if (publisher == null) {
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }

        Content toBeRevoked = Iterables.getOnlyElement(getContent(publisher, uri));
        remoteClient.revoke(toBeRevoked);

        sendOkResponse(response, "Revoke for " + uri + " sent sucessfully");
    }
    
    @RequestMapping(value="/feeds/youview/{publisher}/unrevoke", method = RequestMethod.POST)
    public void unrevokeContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {
        
        Publisher publisher = Publisher.valueOf(publisherStr.trim().toUpperCase());
        if (publisher == null) {
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }

        Content toBeRevoked = Iterables.getOnlyElement(getContent(publisher, uri));
        remoteClient.unrevoke(toBeRevoked);

        sendOkResponse(response, "Unrevoke for " + uri + " sent sucessfully");
    }
    
    private Iterable<Content> getContent(Publisher publisher, String contentUri) {
        ResolvedContent resolvedContent = contentResolver.findByCanonicalUris(ImmutableList.of(contentUri));
        Collection<Identified> resolved = resolvedContent.asResolvedMap().values();
        return ImmutableList.of((Content) Iterables.getOnlyElement(resolved));
    }
    
    private void sendOkResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(message.getBytes(Charsets.UTF_8));
    }
}
