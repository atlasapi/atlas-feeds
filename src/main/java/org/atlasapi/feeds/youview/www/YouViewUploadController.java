package org.atlasapi.feeds.youview.www;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.revocation.RevocationProcessor;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
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

// TODO remove all the duplication
@Controller
public class YouViewUploadController {

    private final ContentResolver contentResolver;
    private final GranularYouViewService remoteService;
    private final ContentHierarchyExpander hierarchyExpander;
    private final RevocationProcessor revocationProcessor;
    
    public YouViewUploadController(ContentResolver contentResolver, GranularYouViewService remoteService,
            ContentHierarchyExpander hierarchyExpander, RevocationProcessor revocationProcessor) {
        this.contentResolver = checkNotNull(contentResolver);
        this.remoteService = checkNotNull(remoteService);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
        this.revocationProcessor = checkNotNull(revocationProcessor);
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
            @RequestParam(value = "uri", required = true) String uri,
            @RequestParam(value = "element_id", required = false) String elementId,
            @RequestParam(value = "type", required = false) String typeStr) throws IOException, HttpException {
        
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
        
        Content content = toBeUploaded.get();
        
        if (typeStr != null) {
            if (elementId == null) {
                sendError(response, SC_BAD_REQUEST, "required parameter 'element_id' not specified when uploading an individual TVAnytime element");
                return;
            }

            TVAElementType type = parseTypeFrom(typeStr);
            
            if (type == null) {
                sendError(response, SC_BAD_REQUEST, "Invalid type provided");
                return;
            }
            
            switch(type) {
            case BRAND:
            case ITEM:
            case SERIES:
                remoteService.uploadContent(toBeUploaded.get());
                break;
            case BROADCAST:
                if (!(content instanceof Item)) {
                    sendError(response, SC_BAD_REQUEST, "content must be an Item to upload a Broadcast");
                    return;
                }
                Map<String, ItemBroadcastHierarchy> broadcastHierarchies = hierarchyExpander.broadcastHierarchiesFor((Item) content);
                Optional<ItemBroadcastHierarchy> broadcastHierarchy = Optional.fromNullable(broadcastHierarchies.get(elementId));
                if (!broadcastHierarchy.isPresent()) {
                    sendError(response, SC_BAD_REQUEST, "No Broadcast found with the provided elementId");
                    return;
                }
                remoteService.uploadBroadcast(broadcastHierarchy.get(), elementId);
                break;
            case ONDEMAND:
                if (!(content instanceof Item)) {
                    sendError(response, SC_BAD_REQUEST, "content must be an Item to upload a OnDemand");
                    return;
                }
                Map<String, ItemOnDemandHierarchy> onDemandHierarchies = hierarchyExpander.onDemandHierarchiesFor((Item) content);
                Optional<ItemOnDemandHierarchy> onDemandHierarchy = Optional.fromNullable(onDemandHierarchies.get(elementId));
                if (!onDemandHierarchy.isPresent()) {
                    sendError(response, SC_BAD_REQUEST, "No OnDemand found with the provided elementId");
                    return;
                }
                remoteService.uploadOnDemand(onDemandHierarchy.get(), elementId);
                break;
            case VERSION:
                if (!(content instanceof Item)) {
                    sendError(response, SC_BAD_REQUEST, "content must be an Item to upload a Version");
                    return;
                }
                Map<String, ItemAndVersion> versionHierarchies = hierarchyExpander.versionHierarchiesFor((Item) content);
                Optional<ItemAndVersion> versionHierarchy = Optional.fromNullable(versionHierarchies.get(elementId));
                if (!versionHierarchy.isPresent()) {
                    sendError(response, SC_BAD_REQUEST, "No Version found with the provided elementId");
                    return;
                }
                remoteService.uploadVersion(versionHierarchy.get(), elementId);
                break;
            default:
                sendError(response, SC_BAD_REQUEST, "Invalid type provided");
                return;
            }
        } else {
            remoteService.uploadContent(toBeUploaded.get());
            if (content instanceof Item) {
                Map<String, ItemAndVersion> versions = hierarchyExpander.versionHierarchiesFor((Item) content);
                for (Entry<String, ItemAndVersion> version : versions.entrySet()) {
                    remoteService.uploadVersion(version.getValue(), version.getKey());
                }
                Map<String, ItemBroadcastHierarchy> broadcasts = hierarchyExpander.broadcastHierarchiesFor((Item) content);
                for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcasts.entrySet()) {
                    remoteService.uploadBroadcast(broadcast.getValue(), broadcast.getKey());
                }
                Map<String, ItemOnDemandHierarchy> onDemands = hierarchyExpander.onDemandHierarchiesFor((Item) content);
                for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
                    remoteService.uploadOnDemand(onDemand.getValue(), onDemand.getKey());
                }
            }
        }
        
        sendOkResponse(response, "Upload for " + uri + " sent sucessfully");
    }
    
    private TVAElementType parseTypeFrom(String typeStr) {
        for (TVAElementType type : TVAElementType.values()) {
            if (type.name().equalsIgnoreCase(typeStr.trim())) {
                return type;
            }
        }
        return null;
    }

    /**
     * Sends deletes for the XML elements representing a particular piece of content to YouView
     * @param uri the canonical uri for the item to delete
     * @throws IOException 
     */    
    @RequestMapping(value="/feeds/youview/{publisher}/delete", method = RequestMethod.POST)
    public void deleteContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri,
            @RequestParam(value = "element_id", required = true) String elementId,
            @RequestParam(value = "type", required = true) String typeStr) throws IOException {

        Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
        if (!publisher.isPresent()) {
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }
        if (elementId == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'element_id' not specified");
            return;
        }
        if (typeStr == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'type' not specified");
            return;
        }
        Optional<Content> toBeDeleted = getContent(publisher.get(), uri);
        if (!toBeDeleted.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "content does not exist");
            return;
        }
        
        TVAElementType type = parseTypeFrom(typeStr);
        if (type == null) {
            sendError(response, SC_BAD_REQUEST, "Invalid type provided");
            return;
        }
        
        remoteService.sendDeleteFor(toBeDeleted.get(), type, elementId);

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
        revocationProcessor.revoke(toBeRevoked.get());

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
        revocationProcessor.unrevoke(toBeUnrevoked.get());

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
