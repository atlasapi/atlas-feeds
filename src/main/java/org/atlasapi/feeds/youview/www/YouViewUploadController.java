package org.atlasapi.feeds.youview.www;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.creation.TaskCreator;
import org.atlasapi.feeds.tasks.youview.processing.TaskProcessor;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.feeds.youview.revocation.RevocationProcessor;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.joda.time.DateTime;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.webapp.query.DateTimeInQueryParser;

// TODO remove all the duplication
@Controller
public class YouViewUploadController {

    private final ContentResolver contentResolver;
    private final TaskCreator taskCreator;
    private final TaskStore taskStore;
    private final PayloadCreator payloadCreator;
    private final ContentHierarchyExpander hierarchyExpander;
    private final RevocationProcessor revocationProcessor;
    private final Clock clock;
    private final TaskProcessor taskProcessor;
    private final DateTimeInQueryParser dateTimeInQueryParser;
    private final ScheduleResolver scheduleResolver;
    private final ChannelResolver channelResolver;
    private final SubstitutionTableNumberCodec channelIdCodec;
    
    public YouViewUploadController(
            ContentResolver contentResolver,
            TaskCreator taskCreator,
            TaskStore taskStore,
            PayloadCreator payloadCreator,
            ContentHierarchyExpander hierarchyExpander,
            RevocationProcessor revocationProcessor,
            TaskProcessor taskProcessor, 
            ScheduleResolver scheduleResolver,
            ChannelResolver channelResolver,
            Clock clock
    ) {
        this.contentResolver = checkNotNull(contentResolver);
        this.taskCreator = checkNotNull(taskCreator);
        this.taskStore = checkNotNull(taskStore);
        this.payloadCreator = checkNotNull(payloadCreator);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
        this.revocationProcessor = checkNotNull(revocationProcessor);
        this.taskProcessor = checkNotNull(taskProcessor);
        this.clock = checkNotNull(clock);
        this.dateTimeInQueryParser = new DateTimeInQueryParser();
        this.scheduleResolver = checkNotNull(scheduleResolver);
        this.channelResolver = checkNotNull(channelResolver);
        this.channelIdCodec = new SubstitutionTableNumberCodec();
    }
    
    @RequestMapping(value="/feeds/youview/{publisher}/schedule/upload")
    public void uploadSchedule(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam("channel") String channelStr,
            @RequestParam("from") String fromStr,
            @RequestParam("to") String toStr
            ) throws IOException {
        
        DateTime from = dateTimeInQueryParser.parse(fromStr);
        DateTime to = dateTimeInQueryParser.parse(toStr);
        Channel channel = channelResolver.fromId(channelIdCodec.decode(channelStr).longValue())
                                         .requireValue();
        
        Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
        
        Schedule schedule = scheduleResolver.unmergedSchedule(
                                                    from, 
                                                    to, 
                                                    ImmutableSet.of(channel), 
                                                    ImmutableSet.of(publisher.get())
                                             );
        
        List<Item> items = Iterables.getOnlyElement(schedule.scheduleChannels()).items();
        
        StringBuilder sb = new StringBuilder();
        for(Item item : items) {
            try {
                sb.append("Uploading " + item.getCanonicalUri() + System.lineSeparator());
                uploadContent(true, item);
                sb.append("Done uploading " + item.getCanonicalUri() + System.lineSeparator());
            } catch (PayloadGenerationException e) {
                sb.append("Error uploading " + e.getMessage());
            }
        }
        sendOkResponse(response, sb.toString());
    }
        
    /**
     * Uploads the XML for a particular piece of content to YouView
     * @param uri the canonical uri for the item to upload
     * @throws IOException
     * @throws HttpException 
     */
    // TODO this method does far too much right now
    @RequestMapping(value="/feeds/youview/{publisher}/upload", method = RequestMethod.POST)
    public void uploadContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri,
            @RequestParam(value = "element_id", required = false) String elementId,
            @RequestParam(value = "type", required = false) String typeStr,
            @RequestParam(value = "immediate", required = false, defaultValue = "false") boolean immediate) throws IOException, HttpException, PayloadGenerationException {
        
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

            Payload payload = payloadCreator.payloadFrom(hierarchyExpander.contentCridFor(content), content);

            switch(type) {
            case BRAND:
            case ITEM:
            case SERIES:
                processTask(taskCreator.taskFor(elementId, toBeUploaded.get(), payload, Action.UPDATE), immediate);
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
                processTask(taskCreator.taskFor(elementId, broadcastHierarchy.get(), payload, Action.UPDATE), immediate);
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
                processTask(taskCreator.taskFor(elementId, onDemandHierarchy.get(), payload, Action.UPDATE), immediate);
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
                processTask(taskCreator.taskFor(elementId, versionHierarchy.get(), payload, Action.UPDATE), immediate);
                break;
            default:
                sendError(response, SC_BAD_REQUEST, "Invalid type provided");
                return;
            }
            
        } else {
            uploadContent(immediate, content);
        }
        
        sendOkResponse(response, "Upload for " + uri + " sent sucessfully");
    }

    private void uploadContent(boolean immediate, Content content)
            throws PayloadGenerationException {
        Payload p = payloadCreator.payloadFrom(hierarchyExpander.contentCridFor(content), content);
        Task task = taskCreator.taskFor(hierarchyExpander.contentCridFor(content), content, p, Action.UPDATE);
        processTask(task, immediate);

        if (content instanceof Item) {
            Map<String, ItemAndVersion> versions = hierarchyExpander.versionHierarchiesFor((Item) content);
            for (Entry<String, ItemAndVersion> version : versions.entrySet()) {
                Payload versionPayload = payloadCreator.payloadFrom(version.getKey(), version.getValue());
                Task versionTask = taskCreator.taskFor(version.getKey(), version.getValue(), versionPayload, Action.UPDATE);
                processTask(versionTask, immediate);
            }
            Map<String, ItemBroadcastHierarchy> broadcasts = hierarchyExpander.broadcastHierarchiesFor((Item) content);
            for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcasts.entrySet()) {
                Optional<Payload> broadcastPayload = payloadCreator.payloadFrom(broadcast.getKey(), broadcast.getValue());
                if (broadcastPayload.isPresent()) {
                    Task bcastTask = taskCreator.taskFor(broadcast.getKey(), broadcast.getValue(), broadcastPayload.get(), Action.UPDATE);
                    processTask(bcastTask, immediate);
                }
            }
            Map<String, ItemOnDemandHierarchy> onDemands = hierarchyExpander.onDemandHierarchiesFor((Item) content);
            for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
                Payload odPayload = payloadCreator.payloadFrom(onDemand.getKey(), onDemand.getValue());
                Task odTask = taskCreator.taskFor(onDemand.getKey(), onDemand.getValue(), odPayload, Action.UPDATE);
                processTask(odTask, immediate);
            }
        }
    }
    
    private void processTask(Task task, boolean immediate) {
        if (task == null) {
            return;
        }
        Task savedTask = taskStore.save(task);

        if (immediate) {
            taskProcessor.process(savedTask);
        }
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
        
        // TODO ideally this would go via the TaskCreator, but that would require resolving 
        // the hierarchies for each type of element
        Destination destination = new YouViewDestination(toBeDeleted.get().getCanonicalUri(), type, elementId);
        Task task = Task.builder()
                .withAction(Action.DELETE)
                .withDestination(destination)
                .withCreated(clock.now())
                .withPublisher(toBeDeleted.get().getPublisher())
                .withStatus(Status.NEW)
                .build();
        taskStore.save(task);

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

        ImmutableList<Task> revocationTasks = revocationProcessor.revoke(toBeRevoked.get());
        for (Task revocationTask : revocationTasks) {
            processTask(revocationTask, true);
        }

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

        ImmutableList<Task> revocationTasks = revocationProcessor.unrevoke(toBeUnrevoked.get());
        for (Task revocationTask : revocationTasks) {
            processTask(revocationTask, true);
        }

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
