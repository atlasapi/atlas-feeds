package org.atlasapi.feeds.youview.www;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.feeds.youview.revocation.RevocationProcessor;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.reporting.telescope.AtlasFeedsReporters;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

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
import org.apache.commons.lang3.StringUtils;
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
    private final IdGenerator idGenerator;
    private final TaskProcessor nitroTaskProcessor;

    public static Builder builder() {
        return new Builder();
    }

    private YouViewUploadController(
            ContentResolver contentResolver,
            TaskCreator taskCreator,
            TaskStore taskStore,
            PayloadCreator payloadCreator,
            ContentHierarchyExpander hierarchyExpander,
            RevocationProcessor revocationProcessor,
            TaskProcessor taskProcessor,
            ScheduleResolver scheduleResolver,
            ChannelResolver channelResolver,
            IdGenerator idGenerator,
            Clock clock,
            TaskProcessor nitroTaskProcessor
    ) {
        this.contentResolver = checkNotNull(contentResolver);
        this.taskCreator = checkNotNull(taskCreator);
        this.taskStore = checkNotNull(taskStore);
        this.payloadCreator = checkNotNull(payloadCreator);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
        this.revocationProcessor = checkNotNull(revocationProcessor);
        this.taskProcessor = checkNotNull(taskProcessor);
        this.clock = checkNotNull(clock);
        this.scheduleResolver = checkNotNull(scheduleResolver);
        this.channelResolver = checkNotNull(channelResolver);
        this.idGenerator = checkNotNull(idGenerator);
        this.nitroTaskProcessor = checkNotNull(nitroTaskProcessor);

        this.dateTimeInQueryParser = new DateTimeInQueryParser();
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        this.channelIdCodec = new SubstitutionTableNumberCodec();
    }

    private void handleChannel(
            HttpServletResponse response,
            String channelStr,
            FeedsTelescopeReporter telescope
    ) throws IOException, PayloadGenerationException {
        Channel channel = channelResolver.fromUri(channelStr).requireValue();

        if (!channel.getBroadcaster().key().equals("bbc.co.uk")) {
            sendError(response, SC_BAD_REQUEST, "Only BBC channels can be uploaded");
            return;
        }

        uploadChannel(true, channel, false, telescope);
    }

    private void handleMasterbrand(
            HttpServletResponse response,
            String channelStr,
            FeedsTelescopeReporter telescope
    ) throws IOException, PayloadGenerationException {
        Channel channel = channelResolver.fromUri(channelStr).requireValue();

        if (!channel.getBroadcaster().key().equals("bbc.co.uk")) {
            sendError(response, SC_BAD_REQUEST, "Only BBC channels can be uploaded");
            return;
        }

        uploadChannel(true, channel, true, telescope);
    }

    @RequestMapping(value = "/feeds/youview/{publisher}/schedule/upload")
    public void uploadSchedule(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam("channel") String channelStr,
            @RequestParam("from") String fromStr,
            @RequestParam("to") String toStr
    ) throws IOException {
        FeedsTelescopeReporter telescope = FeedsTelescopeReporter.create(AtlasFeedsReporters.YOU_VIEW_SCHEDULE_UPLOADER);
        telescope.startReporting();

        try {
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
            for (Item item : items) {
                try {
                    sb.append("Uploading " + item.getCanonicalUri() + System.lineSeparator());
                    uploadContent(true, item, telescope);
                    sb.append("Done uploading " + item.getCanonicalUri() + System.lineSeparator());
                } catch (PayloadGenerationException e) {
                    telescope.reportFailedEvent(
                            MAPPER.writeValueAsString(item) +" failed to upload. "
                            + "(" + (e.toString() + ")" )
                    );
                    sb.append("Error uploading " + e.getMessage());
                }
            }
            sendOkResponse(response, sb.toString());
        } catch (Exception e) {
            telescope.reportFailedEvent(
                    "The call to /feeds/youview/"+publisherStr+"/schedule/upload"
                    + " Params: channel="+channelStr+", from="+fromStr+", to="+toStr
                    + " failed. (" + e.toString() + ")");
            telescope.endReporting();
            throw e;
        }
    }

    @RequestMapping(value = "/feeds/youview/bbc_nitro/upload/multi")
    public void uploadMultipleContent(HttpServletRequest request, HttpServletResponse response)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        FeedsTelescopeReporter telescope = FeedsTelescopeReporter.create(AtlasFeedsReporters.YOU_VIEW_BBC_MULTI_UPLOADER);
        telescope.startReporting();

        try{
            List<String> uris = MAPPER.readValue(request.getInputStream(), STRING_LIST);

            if (uris.isEmpty()) {
                sendError(response, SC_BAD_REQUEST, "URI list is empty");

                telescope.reportFailedEvent(
                        "The call to " + request.getRequestURL() + " failed. The URI list is empty.");
                return;
            }

            List<ListenableFuture<Try>> responses = Lists.newArrayList();
            for (final String uri : uris) {
                ListenableFuture<Try> task = executor.submit(() -> {
                    try {
                        Optional<Content> content = getContent(uri);
                        if (!content.isPresent()) {
                            telescope.reportFailedEvent("No content was found at uri " + uri);
                            return Try.exception(new IllegalArgumentException(String.format(
                                    "Content %s not found",
                                    uri
                            )));
                        } else {
                            uploadContent(true, content.get(), telescope);
                            return Try.success(uri);
                        }
                    } catch (Exception e) {
                        telescope.reportFailedEvent(
                                "Content at uri " + uri + " failed to upload. "
                                + "(" + (e.toString() + ")" )
                        );
                        return Try.exception(e);
                    }
                });

                responses.add(task);
            }
            ListenableFuture<List<Try>> allTasks = Futures.allAsList(responses);
            List<Try> allResponses = allTasks.get();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            MAPPER.writeValue(response.getOutputStream(), allResponses);
        } catch (Exception e) {
            telescope.reportFailedEvent(
                    "The call to " + request.getRequestURL() + " failed. "
                    + "(" + e.toString() + ")");
            telescope.endReporting();
            throw e;
        }
    }

    /**
     * Uploads the XML for a particular piece of content to YouView
     *
     * @param uri the canonical uri for the item to upload
     * @throws IOException
     * @throws HttpException
     */
    // TODO this method does far too much right now.
    // I'd argue its not just this method, its the whole class.
    @RequestMapping(value = "/feeds/youview/{publisher}/upload", method = RequestMethod.POST)
    public void uploadContent(
            HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri") String uri,
            @RequestParam(value = "element_id", required = false) String elementId,
            @RequestParam(value = "type", required = false) String typeStr,
            @RequestParam(value = "immediate", required = false, defaultValue = "false")
                    boolean immediate
    ) throws IOException, HttpException, PayloadGenerationException {

        FeedsTelescopeReporter telescope = FeedsTelescopeReporter.create(AtlasFeedsReporters.YOU_VIEW_XML_UPLOADER);
        if(immediate){ //only start reporting if we will actually upload stuff as well.
            //I believe if this is not immediate it will schedule a task, and things will be
            //reported when the task is executed.
            telescope.startReporting();
        }
        try {
            Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
            if (!publisher.isPresent()) {
                sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
                return;
            }

            if (StringUtils.isEmpty(uri)) {
                sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
                return;
            }

            if (isMasterbrandUri(uri)) {
                handleMasterbrand(response, uri, telescope);
            } else if (isServiceUri(uri)) {
                handleChannel(response, uri, telescope);
            } else {
                handleContent(uri, elementId, typeStr, immediate, response, telescope);
            }


            sendOkResponse(response, "Upload for " + uri + " sent successfully");
        } catch (Exception e){
            if (immediate) { //to prevent logging an error message if it is not started.
                telescope.reportFailedEvent(
                        "The call to /feeds/youview/"+publisherStr+"/upload"
                        + " Params: uri="+uri+", element_id="+elementId+", type="+typeStr+", immediate="+immediate
                        + " failed. (" + e.toString() + ")");
                telescope.endReporting();
            }
            throw e;
        }
    }

    private boolean isServiceUri(String uri) {
        String[] parts = uri.split("/");
        return parts.length == 5 && "services".equals(parts[parts.length - 2]);
    }

    private boolean isMasterbrandUri(String uri) {
        String[] parts = uri.split("/");
        return parts.length == 5 && "masterbrands".equals(parts[parts.length - 2]);
    }

    private void handleContent(
            String uri,
            String elementId,
            @Nullable String typeStr,
            boolean immediate,
            HttpServletResponse response,
            FeedsTelescopeReporter telescope
    ) throws IOException, PayloadGenerationException {
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

            Payload payload = payloadCreator.payloadFrom(
                    hierarchyExpander.contentCridFor(content),
                    content
            );

            switch (type) {
            case BRAND:
            case ITEM:
            case SERIES:
                handleContent(elementId, immediate, response, toBeUploaded, payload, telescope);
                break;
            case BROADCAST:
                handleBroadcast(elementId, immediate, response, content, telescope);
                break;
            case ONDEMAND:
                handleOnDemand(elementId, immediate, response, content, payload, telescope);
                break;
            case VERSION:
                handleVersion(elementId, immediate, response, content, payload, telescope);
                break;
            default:
                sendError(response, SC_BAD_REQUEST, "Invalid type provided");
            }
        } else {
            uploadContent(immediate, content, telescope);
        }
    }

    private void handleContent(
            @Nullable String elementId,
            boolean immediate,
            HttpServletResponse response,
            Optional<Content> toBeUploaded,
            Payload payload,
            FeedsTelescopeReporter telescope
    ) throws IOException {
        if (elementId == null) {
            sendError(
                    response,
                    SC_BAD_REQUEST,
                    "required parameter 'element_id' not specified when uploading an individual TVAnytime element"
            );
            return;
        }
        log.info("Creating task to process (series?). Atlasid should be {} ",toBeUploaded.get().getId());
        processTask(
                taskCreator.taskFor(elementId, toBeUploaded.get(), payload, Action.UPDATE),
                immediate, telescope
        );
    }

    private void handleVersion(String elementId, boolean immediate, HttpServletResponse response,
            Content content, Payload payload, FeedsTelescopeReporter telescope) throws IOException {
        if (!(content instanceof Item)) {
            sendError(response, SC_BAD_REQUEST, "content must be an Item to upload a Version");
            return;
        }
        Map<String, ItemAndVersion> versionHierarchies = hierarchyExpander.versionHierarchiesFor((Item) content);
        Optional<ItemAndVersion> versionHierarchy = Optional.fromNullable(versionHierarchies.get(
                elementId));
        if (!versionHierarchy.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "No Version found with the provided elementId");
            return;
        }
        log.info("Creating task to process (version?). Atlasid should be {} ",versionHierarchy.get().item().getId());
        processTask(
                taskCreator.taskFor(elementId, versionHierarchy.get(), payload, Action.UPDATE),
                immediate, telescope
        );
    }

    private void handleOnDemand(String elementId, boolean immediate, HttpServletResponse response,
            Content content, Payload payload, FeedsTelescopeReporter telescope) throws IOException {
        if (!(content instanceof Item)) {
            sendError(response, SC_BAD_REQUEST, "content must be an Item to upload a OnDemand");
            return;
        }
        Map<String, ItemOnDemandHierarchy> onDemandHierarchies = hierarchyExpander.onDemandHierarchiesFor(
                (Item) content);
        Optional<ItemOnDemandHierarchy> onDemandHierarchy = Optional.fromNullable(
                onDemandHierarchies.get(elementId));
        if (!onDemandHierarchy.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "No OnDemand found with the provided elementId");
            return;
        }
        log.info("Creating task to process (hierarcy?). Atlasid should be {} ",onDemandHierarchy.get().item().getId());

        processTask(
                taskCreator.taskFor(elementId, onDemandHierarchy.get(), payload, Action.UPDATE),
                immediate, telescope
        );
    }

    private void handleBroadcast(String elementId, boolean immediate, HttpServletResponse response,
            Content content, FeedsTelescopeReporter telescope) throws IOException, PayloadGenerationException {
        if (!(content instanceof Item)) {
            sendError(response, SC_BAD_REQUEST, "content must be an Item to upload a Broadcast");
            return;
        }
        Map<String, ItemBroadcastHierarchy> broadcastHierarchies = hierarchyExpander.broadcastHierarchiesFor(
                (Item) content);

        if (!Strings.isNullOrEmpty(elementId)) {
            ItemBroadcastHierarchy broadcastHierarchy = broadcastHierarchies.get(elementId);
            if (broadcastHierarchy == null) {
                sendError(response, SC_BAD_REQUEST, "No element found with ID " + elementId);
                return;
            }
            uploadBroadcast(elementId, broadcastHierarchy, immediate, telescope);
        } else {
            for (Entry<String, ItemBroadcastHierarchy> broadcastHierarchy : broadcastHierarchies.entrySet()) {
                uploadBroadcast(
                        broadcastHierarchy.getKey(),
                        broadcastHierarchy.getValue(),
                        immediate,
                        telescope
                );
            }
        }
    }

    private void uploadContent(boolean immediate, Content content, FeedsTelescopeReporter telescope)
            throws PayloadGenerationException {
        if (immediate) {
            log.info("Force uploading content {}", content.getCanonicalUri());
        }
        Payload p = payloadCreator.payloadFrom(hierarchyExpander.contentCridFor(content), content);
        Task task = taskCreator.taskFor(
                hierarchyExpander.contentCridFor(content),
                content,
                p,
                Action.UPDATE
        );
        processTask(task, immediate, telescope);

        if (content instanceof Item) {
            Map<String, ItemAndVersion> versions = hierarchyExpander.versionHierarchiesFor((Item) content);
            for (Entry<String, ItemAndVersion> version : versions.entrySet()) {
                Payload versionPayload = payloadCreator.payloadFrom(
                        version.getKey(),
                        version.getValue()
                );
                Task versionTask = taskCreator.taskFor(
                        version.getKey(),
                        version.getValue(),
                        versionPayload,
                        Action.UPDATE
                );
                processTask(versionTask, immediate, telescope);
            }
            Map<String, ItemBroadcastHierarchy> broadcasts = hierarchyExpander.broadcastHierarchiesFor(
                    (Item) content);
            for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcasts.entrySet()) {
                Optional<Payload> broadcastPayload = payloadCreator.payloadFrom(
                        broadcast.getKey(),
                        broadcast.getValue()
                );
                if (broadcastPayload.isPresent()) {
                    Task bcastTask = taskCreator.taskFor(
                            broadcast.getKey(),
                            broadcast.getValue(),
                            broadcastPayload.get(),
                            Action.UPDATE
                    );
                    processTask(bcastTask, immediate, telescope);
                }
            }
            Map<String, ItemOnDemandHierarchy> onDemands = hierarchyExpander.onDemandHierarchiesFor(
                    (Item) content);
            for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
                ItemOnDemandHierarchy onDemandHierarchy = onDemand.getValue();
                Location location = onDemandHierarchy.location();
                Action action = location.getAvailable() ? Action.UPDATE : Action.DELETE;

                Payload odPayload = payloadCreator.payloadFrom(
                        onDemand.getKey(),
                        onDemandHierarchy
                );
                Task odTask = taskCreator.taskFor(
                        onDemand.getKey(),
                        onDemandHierarchy,
                        odPayload,
                        action
                );
                processTask(odTask, immediate, telescope);
            }

            if (immediate) {
                Item item = (Item) content;
                resolveAndUploadParent(item.getContainer(), true, telescope);

                if (item instanceof Episode) {
                    Episode episode = (Episode) item;
                    resolveAndUploadParent(episode.getSeriesRef(), true, telescope);
                }
            }
        }
    }

    private void uploadChannel(boolean immediate, Channel channel, boolean masterbrand, FeedsTelescopeReporter telescope)
            throws PayloadGenerationException {
        Payload p = payloadCreator.payloadFrom(channel, masterbrand);
        Task task = taskCreator.taskFor(
                idGenerator.generateChannelCrid(channel),
                channel,
                p,
                Action.UPDATE
        );
        processChannelTask(task, immediate, telescope);
    }

    private void resolveAndUploadParent(ParentRef ref, boolean immediate, FeedsTelescopeReporter telescope)
            throws PayloadGenerationException {
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
            processTask(parentTask, immediate, telescope);
        }
    }

    private void processTask(
            @Nullable Task task,
            boolean immediate,
            FeedsTelescopeReporter telescope
    ) {
        if (task == null) {
            return;
        }
        Task savedTask = taskStore.save(Task.copy(task).withManuallyCreated(true).build());

        if (immediate) {
            taskProcessor.process(savedTask, telescope);
        }
    }

    private void processChannelTask(Task task, boolean immediate, FeedsTelescopeReporter telescope) {
        if (task == null) {
            return;
        }
        Task savedTask = taskStore.save(Task.copy(task).withManuallyCreated(true).build());

        if (immediate) {
            nitroTaskProcessor.process(savedTask, telescope);
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
     *
     * @param uri the canonical uri for the item to delete
     * @throws IOException
     */
    @RequestMapping(value = "/feeds/youview/{publisher}/delete", method = RequestMethod.POST)
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
        Destination destination = new YouViewDestination(
                toBeDeleted.get().getCanonicalUri(),
                type,
                elementId
        );
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
     *
     * @param uri the canonical uri for the item to revoke
     * @throws IOException
     */
    @RequestMapping(value = "/feeds/youview/{publisher}/revoke", method = RequestMethod.POST)
    public void revokeContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {

        FeedsTelescopeReporter telescope = FeedsTelescopeReporter.create(AtlasFeedsReporters.YOU_VIEW_REVOKER);
        telescope.startReporting();
        String url = "/feeds/youview/" + publisherStr + "/revoke/" + "?uri" + uri;

        try {
            Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
            if (!publisher.isPresent()) {
                telescope.reportFailedEvent(
                        "The call to " + url
                        + " failed because publisher "+ publisherStr+" was not found.");
                telescope.endReporting();
                sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
                return;
            }
            if (uri == null) {
                telescope.reportFailedEvent(
                        "The call to " + url
                        + " failed because the uri was not specified.");
                telescope.endReporting();
                sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
                return;
            }
            Optional<Content> toBeRevoked = getContent(uri);
            if (!toBeRevoked.isPresent()) {
                telescope.reportFailedEvent(
                        "The call to " + url
                        + " failed because the content does not exist.");
                telescope.endReporting();
                sendError(response, SC_BAD_REQUEST, "content does not exist");
                return;
            }

            ImmutableList<Task> revocationTasks = revocationProcessor.revoke(toBeRevoked.get());
            for (Task revocationTask : revocationTasks) {
                processTask(revocationTask, true, telescope);
            }

            sendOkResponse(response, "Revoke for " + uri + " sent sucessfully");
        } catch(Exception e) { telescope.reportFailedEvent(
                "The call to " + url
                + " failed. (" + e.toString() + ")");
            telescope.endReporting();
            throw e;
        }
    }

    /**
     * Unrevokes a particular piece of content from the YouView system.
     *
     * @param uri the canonical uri for the item to unrevoke
     * @throws IOException
     */
    @RequestMapping(value = "/feeds/youview/{publisher}/unrevoke", method = RequestMethod.POST)
    public void unrevokeContent(HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {

        FeedsTelescopeReporter telescope = FeedsTelescopeReporter.create(AtlasFeedsReporters.YOU_VIEW_UNREVOKER);
        telescope.startReporting();
        String url = "/feeds/youview/" + publisherStr + "/unrevoke/" + "?uri" + uri;

        try {
            Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
            if (!publisher.isPresent()) {
                telescope.reportFailedEvent(
                    "The call to " + url
                    + " failed because publisher "+ publisherStr+" was not found.");
                telescope.endReporting();
                sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
                return;
            }
            if (uri == null) {
                telescope.reportFailedEvent(
                        "The call to " + url
                        + " failed because the uri was not specified.");
                telescope.endReporting();
                sendError(response, SC_BAD_REQUEST, "required parameter 'uri' not specified");
                return;
            }
            Optional<Content> toBeUnrevoked = getContent(uri);
            if (!toBeUnrevoked.isPresent()) {
                telescope.reportFailedEvent(
                        "The call to " + url
                        + " failed because the content does not exist.");
                sendError(response, SC_BAD_REQUEST, "content does not exist");
                return;
            }

            ImmutableList<Task> revocationTasks = revocationProcessor.unrevoke(toBeUnrevoked.get());
            for (Task revocationTask : revocationTasks) {
                processTask(revocationTask, true, telescope);
            }

            sendOkResponse(response, "Unrevoke for " + uri + " sent sucessfully");
        } catch(Exception e) {
            telescope.reportFailedEvent(
                    "The call to " + url + " failed. (" + e.toString() + ")");
            telescope.endReporting();
            throw e;
        }
    }

    private void uploadBroadcast(String elementId, ItemBroadcastHierarchy broadcastHierarchy,
            boolean immediate, FeedsTelescopeReporter telescope) throws PayloadGenerationException {
        Optional<Payload> bcastPayload = payloadCreator.payloadFrom(elementId, broadcastHierarchy);
        if (!bcastPayload.isPresent()) {
            // a lack of payload is because no BroadcastEvent should be generated,
            // likely because of BroadcastEvent deduplication
            return;
        }
        processTask(
                taskCreator.taskFor(
                        elementId,
                        broadcastHierarchy,
                        bcastPayload.get(),
                        Action.UPDATE
                ),
                immediate,
                telescope
        );
    }

    private Optional<Publisher> findPublisher(String publisherStr) {
        for (Publisher publisher : Publisher.all()) {
            if (publisher.name().equals(publisherStr)) {
                return Optional.of(publisher);
            }
        }
        return Optional.absent();
    }

    private void sendError(HttpServletResponse response, int responseCode, String message)
            throws IOException {
        response.sendError(responseCode, message);
        response.setContentLength(0);
    }

    private Optional<Content> getContent(String contentUri) {
        ResolvedContent resolvedContent = contentResolver.findByCanonicalUris(ImmutableList.of(
                contentUri));
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

    public static final class Builder {

        private ContentResolver contentResolver;
        private TaskCreator taskCreator;
        private TaskStore taskStore;
        private PayloadCreator payloadCreator;
        private ContentHierarchyExpander hierarchyExpander;
        private RevocationProcessor revocationProcessor;
        private Clock clock;
        private TaskProcessor taskProcessor;
        private ScheduleResolver scheduleResolver;
        private ChannelResolver channelResolver;
        private IdGenerator idGenerator;
        private TaskProcessor nitroTaskProcessor;

        private Builder() {
        }

        public Builder withContentResolver(ContentResolver val) {
            contentResolver = val;
            return this;
        }

        public Builder withTaskCreator(TaskCreator val) {
            taskCreator = val;
            return this;
        }

        public Builder withTaskStore(TaskStore val) {
            taskStore = val;
            return this;
        }

        public Builder withPayloadCreator(PayloadCreator val) {
            payloadCreator = val;
            return this;
        }

        public Builder withHierarchyExpander(ContentHierarchyExpander val) {
            hierarchyExpander = val;
            return this;
        }

        public Builder withRevocationProcessor(RevocationProcessor val) {
            revocationProcessor = val;
            return this;
        }

        public Builder withClock(Clock val) {
            clock = val;
            return this;
        }

        public Builder withTaskProcessor(TaskProcessor val) {
            taskProcessor = val;
            return this;
        }

        public Builder withScheduleResolver(ScheduleResolver val) {
            scheduleResolver = val;
            return this;
        }

        public Builder withChannelResolver(ChannelResolver val) {
            channelResolver = val;
            return this;
        }

        public Builder withIdGenerator(IdGenerator val) {
            idGenerator = val;
            return this;
        }

        public Builder withNitroTaskProcessor(TaskProcessor val) {
            nitroTaskProcessor = val;
            return this;
        }

        public YouViewUploadController build() {
            return new YouViewUploadController(
                    contentResolver,
                    taskCreator,
                    taskStore,
                    payloadCreator,
                    hierarchyExpander,
                    revocationProcessor,
                    taskProcessor,
                    scheduleResolver,
                    channelResolver,
                    idGenerator,
                    clock,
                    nitroTaskProcessor
            );
        }
    }
}
