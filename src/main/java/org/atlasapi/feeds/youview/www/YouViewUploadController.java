package org.atlasapi.feeds.youview.www;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
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
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.ScheduleResolver;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.webapp.query.DateTimeInQueryParser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

// TODO remove all the duplication
@Controller
public class YouViewUploadController {

    private static final Logger log = LoggerFactory.getLogger(YouViewUploadController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.registerModule(new GuavaModule());
    }
    private static final JavaType STRING_LIST = MAPPER.getTypeFactory()
            .constructCollectionType(List.class, String.class);

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
    private final ListeningExecutorService executor;
    
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

        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
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

    @RequestMapping(value="/feeds/youview/bbc_nitro/upload/multi")
    public void uploadMultipleContent(HttpServletRequest request, HttpServletResponse response)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        List<String> uris = MAPPER.readValue(request.getInputStream(), STRING_LIST);

        if (uris.isEmpty()) {
            sendError(response, SC_BAD_REQUEST, "URI list is empty");
            return;
        }

        List<ListenableFuture<Try>> responses = Lists.newArrayList();
        for (final String uri : uris) {
            ListenableFuture<Try> task = executor.submit(new Callable<Try>() {

                @Override
                public Try call() throws Exception {
                    try {
                        Optional<Content> content = getContent(uri);
                        if (!content.isPresent()) {
                            return Try.exception(new IllegalArgumentException(String.format(
                                    "Content %s not found",
                                    uri
                            )));
                        } else {
                            uploadContent(true, content.get());
                            return Try.success(uri);
                        }
                    } catch (Exception e) {
                        return Try.exception(e);
                    }
                }
            });

            responses.add(task);
        }

        ListenableFuture<List<Try>> allTasks = Futures.allAsList(responses);
        List<Try> allResponses = allTasks.get();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        MAPPER.writeValue(response.getOutputStream(), allResponses);
    }

    /**
     * Uploads the XML for a particular piece of content to YouView
     * @param uri the canonical uri for the item to upload
     * @throws IOException
     * @throws HttpException 
     */
    // TODO this method does far too much right now
    @RequestMapping(value="/feeds/youview/{publisher}/upload", method = RequestMethod.POST)
    public void uploadContent(
            HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri") String uri,
            @RequestParam(value = "element_id", required = false) String elementId,
            @RequestParam(value = "type", required = false) String typeStr,
            @RequestParam(value = "immediate", required = false, defaultValue = "false")
                    boolean immediate
    ) throws IOException, HttpException, PayloadGenerationException {
        
        Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
        if (!publisher.isPresent()) {
            sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
            return;
        }
        if (uri == null) {
            sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
            return;
        }
        
        Optional<Content> toBeUploaded = getContent(uri);
        if (!toBeUploaded.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "content does not exist");
            return;
        }
        
        Content content = toBeUploaded.get();
        
        if (typeStr != null) {
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
                if (elementId == null) {
                    sendError(response, SC_BAD_REQUEST, "required parameter 'element_id' not specified when uploading an individual TVAnytime element");
                    return;
                }

                processTask(taskCreator.taskFor(elementId, toBeUploaded.get(), payload, Action.UPDATE), immediate);
                break;

            case BROADCAST:
                if (!(content instanceof Item)) {
                    sendError(response, SC_BAD_REQUEST, "content must be an Item to upload a Broadcast");
                    return;
                }
                Map<String, ItemBroadcastHierarchy> broadcastHierarchies = hierarchyExpander.broadcastHierarchiesFor((Item) content);

                if (!Strings.isNullOrEmpty(elementId)) {
                    ItemBroadcastHierarchy broadcastHierarchy = broadcastHierarchies.get(elementId);
                    if (broadcastHierarchy == null) {
                        sendError(response, SC_BAD_REQUEST, "No element found with ID " + elementId);
                        return;
                    }
                    uploadBroadcast(elementId, broadcastHierarchy, immediate);
                } else {
                    for (Entry<String, ItemBroadcastHierarchy> broadcastHierarchy : broadcastHierarchies.entrySet()) {
                        uploadBroadcast(broadcastHierarchy.getKey(), broadcastHierarchy.getValue(), immediate);
                    }
                }
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
        if (immediate) {
            log.info("Force uploading content {}", content.getCanonicalUri());
        }
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

            if (immediate) {
                Item item = (Item) content;
                resolveAndUploadParent(item.getContainer(), true);

                if (item instanceof Episode) {
                    Episode episode = (Episode) item;
                    resolveAndUploadParent(episode.getSeriesRef(), true);
                }
            }
        }
    }

    private void resolveAndUploadParent(ParentRef ref, boolean immediate) throws PayloadGenerationException {
        if (ref == null) {
            return;
        }

        Optional<Content> series = getContent(ref.getUri());
        if (series.isPresent()) {
            String contentCrid = hierarchyExpander.contentCridFor(series.get());
            Task parentTask = taskCreator.taskFor(
                    contentCrid,
                    series.get(),
                    payloadCreator.payloadFrom(contentCrid, series.get()),
                    Action.UPDATE
            );
            processTask(parentTask, immediate);
        }
    }

    private void processTask(Task task, boolean immediate) {
        if (task == null) {
            return;
        }
        Task savedTask = taskStore.save(Task.copy(task).withManuallyCreated(true).build());

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
        Optional<Content> toBeDeleted = getContent(uri);
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

        Optional<Content> toBeRevoked = getContent(uri);
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
        Optional<Content> toBeUnrevoked = getContent(uri);
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

    private void uploadBroadcast(String elementId, ItemBroadcastHierarchy broadcastHierarchy, boolean immediate) throws PayloadGenerationException {
        Optional<Payload> bcastPayload = payloadCreator.payloadFrom(elementId, broadcastHierarchy);
        if (!bcastPayload.isPresent()) {
            // a lack of payload is because no BroadcastEvent should be generated,
            // likely because of BroadcastEvent deduplication
            return;
        }
        processTask(taskCreator.taskFor(
                elementId,
                broadcastHierarchy,
                bcastPayload.get(),
                Action.UPDATE),
                immediate);
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
    
    private Optional<Content> getContent(String contentUri) {
        ResolvedContent resolvedContent = contentResolver.findByCanonicalUris(ImmutableList.of(contentUri));
        return Optional.fromNullable((Content) resolvedContent.getFirstValue().valueOrNull());
    }
    
    private void sendOkResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(message.getBytes(Charsets.UTF_8));
    }

    private static class Try {

        private final Optional<Exception> exception;
        private final String data;

        private Try(@Nullable String data) {
            this.exception = Optional.absent();
            this.data = data;
        }

        private Try(Exception e) {
            this.exception = Optional.fromNullable(e);
            this.data = null;
        }

        public static Try exception(Exception e) {
            return new Try(e);
        }

        public static Try success(String data) {
            return new Try(data);
        }

        public boolean isException() {
            return exception.isPresent();
        }

        public Exception getException() {
            return exception.get();
        }

        public String getData() {
            return data;
        }
    }
}
