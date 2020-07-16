package org.atlasapi.feeds.youview.www;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
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
import org.atlasapi.feeds.youview.ContentHierarchyExpanderFactory;
import org.atlasapi.feeds.youview.IdGeneratorFactory;
import org.atlasapi.feeds.youview.YouviewContentMerger;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.feeds.youview.revocation.RevocationProcessor;
import org.atlasapi.feeds.youview.unbox.AmazonContentConsolidator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Quality;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.reporting.telescope.FeedsReporterNames;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporterFactory;

import com.metabroadcast.columbus.telescope.client.EntityType;
import com.metabroadcast.common.base.Maybe;
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.atlasapi.feeds.tasks.youview.creation.DeltaTaskCreationTask.getContributingUris;

// TODO remove all the duplication
@Controller
public class YouViewUploadController {

    private static final Logger log = LoggerFactory.getLogger(YouViewUploadController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private @Autowired ContentHierarchyExpanderFactory contentHierarchyExpanderFactory;
    private @Autowired @Qualifier("YouviewQueryExecutor") KnownTypeQueryExecutor mergingResolver;

    static {
        MAPPER.registerModule(new GuavaModule());
    }

    private static final JavaType STRING_LIST = MAPPER.getTypeFactory()
            .constructCollectionType(List.class, String.class);

    private final ContentResolver contentResolver;
    private final TaskCreator taskCreator;
    private final TaskStore taskStore;
    private final PayloadCreator payloadCreator;
    private final RevocationProcessor revocationProcessor;
    private final Clock clock;
    private final TaskProcessor taskProcessor;
    private final DateTimeInQueryParser dateTimeInQueryParser;
    private final ScheduleResolver scheduleResolver;
    private final ChannelResolver channelResolver;
    private final SubstitutionTableNumberCodec channelIdCodec;
    private final ListeningExecutorService executor;

    public static Builder builder() {
        return new Builder();
    }

    private YouViewUploadController(
            ContentResolver contentResolver,
            TaskCreator taskCreator,
            TaskStore taskStore,
            PayloadCreator payloadCreator,
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
        this.revocationProcessor = checkNotNull(revocationProcessor);
        this.taskProcessor = checkNotNull(taskProcessor);
        this.clock = checkNotNull(clock);
        this.scheduleResolver = checkNotNull(scheduleResolver);
        this.channelResolver = checkNotNull(channelResolver);

        this.dateTimeInQueryParser = new DateTimeInQueryParser();
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        this.channelIdCodec = new SubstitutionTableNumberCodec();
    }

    private void handleChannel(
            HttpServletResponse response,
            String channelStr,
            IdGenerator idGenerator,
            FeedsTelescopeReporter telescope
    ) throws IOException, PayloadGenerationException, IllegalArgumentException, NullPointerException {

        Channel channel = channelResolver.fromUri(channelStr).requireValue();

        if(channel.getBroadcaster() == null || channel.getBroadcaster().key() == null){
            throw new NullPointerException("There was no broadcaster for this channel " + channel);
        }
        if (!channel.getBroadcaster().key().equals("bbc.co.uk")) {
            throw new IllegalArgumentException( "Only BBC channels can be uploaded");
        }

        uploadChannel(true, idGenerator, channel, false, telescope);
    }

    private void handleMasterbrand(
            HttpServletResponse response,
            String channelStr,
            IdGenerator idGenerator,
            FeedsTelescopeReporter telescope
    ) throws IOException, PayloadGenerationException, IllegalArgumentException {

        Channel channel = channelResolver.fromUri(channelStr).requireValue();

        if(channel.getBroadcaster() == null || channel.getBroadcaster().key() == null){
            throw new NullPointerException("There was no broadcaster for this channel " + channel);
        }
        if (!channel.getBroadcaster().key().equals("bbc.co.uk")) {
            throw new IllegalArgumentException("Only BBC channels can be uploaded");
        }

        uploadChannel(true, idGenerator, channel, true, telescope);
    }

    @RequestMapping(value = "/feeds/youview/{publisher}/schedule/upload")
    public void uploadSchedule(HttpServletResponse response, HttpServletRequest request,
            @PathVariable("publisher") String publisherStr,
            @RequestParam("channel") String channelStr,
            @RequestParam("from") String fromStr,
            @RequestParam("to") String toStr
    ) throws IOException {
        FeedsTelescopeReporter telescope = FeedsTelescopeReporterFactory.getInstance()
                .getTelescopeReporter(FeedsReporterNames.YOU_VIEW_MANUAL_SCHEDULE_UPLOADER);
        telescope.startReporting();

        try {
            DateTime from = dateTimeInQueryParser.parse(fromStr);
            DateTime to = dateTimeInQueryParser.parse(toStr);
            Maybe<Channel> channelMaybe =
                    channelResolver.fromId(channelIdCodec.decode(channelStr).longValue());
            if(!channelMaybe.hasValue()){
                throw new IllegalArgumentException("Channel "+channelStr+ " was not found.");
            }

            Channel channel = channelMaybe.requireValue();
            Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
            if(!publisher.isPresent()){
                throw new IllegalArgumentException("Publisher "+publisherStr.trim().toUpperCase()+ " was not found.");
            }

            ContentHierarchyExpander hierarchyExpander =
                    contentHierarchyExpanderFactory.create(publisher.get());

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
                    sb.append("Uploading ").append(item.getCanonicalUri()).append(System.lineSeparator());
                    uploadContent(true, hierarchyExpander, item, telescope);
                    sb.append("Done uploading ").append(item.getCanonicalUri()).append(System.lineSeparator());
                } catch (PayloadGenerationException e) {
                    telescope.reportFailedEvent(
                            "The item below, or one of its derivatives, failed to upload."+
                            MAPPER.writeValueAsString(item)
                            + "(" + e.toString() + ")",
                            EntityType.SCHEDULE.getVerbose()
                    );
                    sb.append("Error uploading ").append(e.getMessage());
                }
            }
            sendOkResponse(response, sb.toString());
        } catch (Exception e) {
            telescope.reportFailedEvent(
                    "The call to " + request.getRequestURI() + " failed. (" + e.toString() + ")",
                    EntityType.SCHEDULE.getVerbose());
            telescope.endReporting();
            throw e;
        }
    }

    /**
     * If this is amazon content, it will not upload the URI itself, but rather find its repId,
     * merge the equivs, and upload the sum (as per normal amazon process).
     */
    @RequestMapping(value = "/feeds/youview/{publisher}/upload/multi")
    public void uploadMultipleContent(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("publisher") String publisherStr)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
        if(!publisher.isPresent()){
            throw new IllegalArgumentException("Publisher "+publisherStr.trim().toUpperCase()+ " was not found.");
        }

        FeedsTelescopeReporter telescope = FeedsTelescopeReporterFactory.getInstance()
                .getTelescopeReporter(FeedsReporterNames.YOU_VIEW_MANUAL_UPLOADER);
        telescope.startReporting();

        ContentHierarchyExpander hierarchyExpander =
                contentHierarchyExpanderFactory.create(publisher.get());

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
                        java.util.Optional<Content> content = getMergedContent(uri);
                        if (!content.isPresent()) {
                            telescope.reportFailedEvent("No content was found at uri " + uri);
                            return Try.exception(new IllegalArgumentException(String.format(
                                    "Content %s not found",
                                    uri
                            )));
                        } else {
                            uploadContent(true, hierarchyExpander, content.get(), telescope);
                            return Try.success(uri);
                        }
                    } catch (Exception e) {
                        telescope.reportFailedEvent(
                                "The content below, or one of its derivatives, failed to upload. "+
                                "URI=" + uri
                                + "(" + e.toString() + ")"
                                , ""
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
        }
        catch (Exception e) {
            telescope.reportFailedEvent(
                    "The call to " + request.getRequestURL() + " failed. "
                    + "(" + e.toString() + ")");
            telescope.endReporting();
            log.error("The call to {}, uri={} failed. ", request.getRequestURL(), e);
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
    @RequestMapping(value = "/feeds/youview/{publisher}/upload", method = RequestMethod.POST)
    public void uploadContent(
            HttpServletResponse response, HttpServletRequest request,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri") String uri,
            @RequestParam(value = "element_id", required = false) String elementId,
            @RequestParam(value = "type", required = false) String typeStr,
            @RequestParam(value = "immediate", required = false, defaultValue = "false")
                    boolean immediate
    ) throws IOException, HttpException, PayloadGenerationException {

        FeedsTelescopeReporter telescope = FeedsTelescopeReporterFactory.getInstance()
                .getTelescopeReporter(FeedsReporterNames.YOU_VIEW_MANUAL_UPLOADER);

        if(immediate){ //only start reporting if we will actually upload stuff as well.
            //I believe if this is not immediate it will schedule a task, and things will be
            //reported when the task is executed.
            telescope.startReporting();
        }
        try {
            Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
            if (!publisher.isPresent()) {
                if (immediate) { //to prevent logging an error message if telescope is not started.
                    telescope.reportFailedEvent(
                            "The call to " + request.getRequestURL()
                            + " has failed. Publisher=" + publisherStr + " not found.");
                    telescope.endReporting();
                }
                sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
                return;
            }

            if (StringUtils.isEmpty(uri)) {
                throw new IllegalArgumentException("required parameter 'uri' not specified");
            }

            if (isMasterbrandUri(uri)) {
                handleMasterbrand(response, uri, IdGeneratorFactory.create(publisher.get()), telescope);
            } else if (isServiceUri(uri)) {
                handleChannel(response, uri, IdGeneratorFactory.create(publisher.get()), telescope);
            } else {
                handleContent(uri, publisher.get(), elementId, typeStr, immediate, response, telescope);
            }

            sendOkResponse(response, "Upload for " + uri + " sent successfully");
            telescope.endReporting();
        }
        catch (Exception e){
            if (immediate) {
                telescope.reportFailedEvent(
                        "The call to " + request.getRequestURL() + " failed. (" + e.toString() + ")");
                telescope.endReporting();
            }
            sendError(response, SC_BAD_REQUEST, e.getMessage());
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

    private void handleContent (
            String uri,
            Publisher publisher,
            String elementId,
            @Nullable String typeStr,
            boolean immediate,
            HttpServletResponse response,
            FeedsTelescopeReporter telescope
    ) throws IOException, PayloadGenerationException, IllegalArgumentException {
        java.util.Optional<Content> toBeUploaded = getMergedContent(uri);
        if (!toBeUploaded.isPresent()) {
            throw new IllegalArgumentException("content does not exist");
        }

        Content content = toBeUploaded.get();

        ContentHierarchyExpander hierarchyExpander = contentHierarchyExpanderFactory.create(publisher);

        if (typeStr != null) {
            TVAElementType type = parseTypeFrom(typeStr);

            if (type == null) {
                throw new IllegalArgumentException("Invalid type provided");
            }

            Payload payload = payloadCreator.payloadFrom(
                    hierarchyExpander.contentCridFor(content),
                    content
            );

            switch (type) {
            case BRAND:
            case ITEM:
            case SERIES:
                handleContent(elementId, immediate, content, payload, telescope);
                break;
            case BROADCAST:
                handleBroadcast(elementId, immediate, hierarchyExpander, response, content, telescope);
                break;
            case ONDEMAND:
                handleOnDemand(elementId, immediate, hierarchyExpander, content, payload, telescope);
                break;
            case VERSION:
                handleVersion(elementId, immediate, hierarchyExpander, content, payload, telescope);
                break;
            default:
                throw new IllegalArgumentException("Invalid type provided");
            }
        } else {
            uploadContent(immediate, hierarchyExpander, content, telescope);
        }
    }

    private void handleContent(
            @Nullable String elementId,
            boolean immediate,
            Content toBeUploaded,
            Payload payload,
            FeedsTelescopeReporter telescope
    ) throws IllegalArgumentException {

        if (elementId == null) {
            throw new NullPointerException("required parameter 'element_id' not specified when uploading an individual TVAnytime element");
        }
        if(toBeUploaded == null ){
            throw new NullPointerException("required parameter 'content to be uploaded' not specified when uploading an individual TVAnytime element");
        }
        log.info("Creating task to process (series?). Atlasid should be {} ",toBeUploaded.getId());
        processTask(
                taskCreator.taskFor(elementId, toBeUploaded, payload, Action.UPDATE),
                immediate, telescope
        );
    }

    private void handleVersion(
            String elementId,
            boolean immediate,
            ContentHierarchyExpander hierarchyExpander,
            Content content,
            Payload payload,
            FeedsTelescopeReporter telescope
    ) throws IllegalArgumentException {

        if (!(content instanceof Item)) {
            throw new IllegalArgumentException( "content must be an Item to upload a Version");
        }
        Map<String, ItemAndVersion> versionHierarchies = hierarchyExpander.versionHierarchiesFor((Item) content);
        Optional<ItemAndVersion> versionHierarchy = Optional.fromNullable(versionHierarchies.get(
                elementId));
        if (!versionHierarchy.isPresent()) {
            throw new IllegalArgumentException( "No Version found with the provided elementId");
        }
        log.info("Creating task to process (version?). Atlasid should be {} ",versionHierarchy.get().item().getId());
        processTask(
                taskCreator.taskFor(elementId, versionHierarchy.get(), payload, Action.UPDATE),
                immediate, telescope
        );
    }

    private void handleOnDemand(
            String elementId,
            boolean immediate,
            ContentHierarchyExpander hierarchyExpander,
            Content content,
            Payload payload,
            FeedsTelescopeReporter telescope
    )  throws IllegalArgumentException {

        if (!(content instanceof Item)) {
            throw new IllegalArgumentException( "content must be an Item to upload a OnDemand");
        }
        Map<String, ItemOnDemandHierarchy> onDemandHierarchies = hierarchyExpander.onDemandHierarchiesFor(
                (Item) content);
        Optional<ItemOnDemandHierarchy> onDemandHierarchy = Optional.fromNullable(
                onDemandHierarchies.get(elementId));
        if (!onDemandHierarchy.isPresent()) {
            throw new IllegalArgumentException( "No OnDemand found with the provided elementId");
        }
        log.info("Creating task to process (hierarcy?). Atlasid should be {} ",onDemandHierarchy.get().item().getId());

        processTask(
                taskCreator.taskFor(elementId, onDemandHierarchy.get(), payload, Action.UPDATE),
                immediate, telescope
        );
    }

    private void handleBroadcast(
            String elementId,
            boolean immediate,
            ContentHierarchyExpander hierarchyExpander,
            HttpServletResponse response,
            Content content,
            FeedsTelescopeReporter telescope
    ) throws PayloadGenerationException, IllegalArgumentException {

        if (!(content instanceof Item)) {
            throw new IllegalArgumentException( "content must be an Item to upload a Broadcast");
        }
        Map<String, ItemBroadcastHierarchy> broadcastHierarchies = hierarchyExpander.broadcastHierarchiesFor(
                (Item) content);

        if (!Strings.isNullOrEmpty(elementId)) {
            ItemBroadcastHierarchy broadcastHierarchy = broadcastHierarchies.get(elementId);
            if (broadcastHierarchy == null) {
                throw new IllegalArgumentException( "No element found with ID " + elementId);
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

    private void uploadContent(
            boolean immediate,
            ContentHierarchyExpander hierarchyExpander,
            Content content,
            FeedsTelescopeReporter telescope
    ) throws PayloadGenerationException {
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
            Map<Action, Item> actionsToProcess = getActionsToProcess((Item) content, Action.UPDATE);
            for (Entry<Action, Item> actionToProcess : actionsToProcess.entrySet()) {
                processUploadContent(
                        immediate,
                        hierarchyExpander,
                        actionToProcess.getValue(),
                        actionToProcess.getKey(),
                        telescope,
                        false
                );
            }

            if(Publisher.AMAZON_UNBOX.equals(content.getPublisher())) {
                Set<Content> forDeletion = extractForDeletion(content);
                for (Content contentToDelete : forDeletion) {
                    actionsToProcess = getActionsToProcess((Item) contentToDelete, Action.DELETE);
                    for (Entry<Action, Item> actionToProcess : actionsToProcess.entrySet()) {
                        processUploadContent(
                                immediate,
                                hierarchyExpander,
                                actionToProcess.getValue(),
                                actionToProcess.getKey(),
                                telescope,
                                true
                        );
                    }
                }
            }
        }
    }

    private void processUploadContent(
            boolean immediate,
            ContentHierarchyExpander hierarchyExpander,
            Content content,
            Action action,
            FeedsTelescopeReporter telescope,
            boolean deleteContent
    ) throws PayloadGenerationException {
        if (deleteContent) {
            Payload p = payloadCreator.payloadFrom(
                    hierarchyExpander.contentCridFor(content),
                    content
            );
            Task task = taskCreator.taskFor(
                    hierarchyExpander.contentCridFor(content),
                    content,
                    p,
                    action
            );
            processTask(task, immediate, telescope);
        }

        Map<String, ItemAndVersion> versions = hierarchyExpander.versionHierarchiesFor((Item) content);
        for (Entry<String, ItemAndVersion> version : versions.entrySet()) {
            try {
                Payload versionPayload = payloadCreator.payloadFrom(
                        version.getKey(),
                        version.getValue()
                );

                Task versionTask = taskCreator.taskFor(
                        version.getKey(),
                        version.getValue(),
                        versionPayload,
                        action
                );
                processTask(versionTask, immediate, telescope);
            } catch (PayloadGenerationException e) {
                e.printStackTrace();
            }
        }
        Map<String, ItemBroadcastHierarchy> broadcasts =
                hierarchyExpander.broadcastHierarchiesFor((Item) content);
        for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcasts.entrySet()) {
            try {
                Optional<Payload> broadcastPayload = payloadCreator.payloadFrom(
                        broadcast.getKey(),
                        broadcast.getValue()
                );

                if (broadcastPayload.isPresent()) {
                    Task bcastTask = taskCreator.taskFor(
                            broadcast.getKey(),
                            broadcast.getValue(),
                            broadcastPayload.get(),
                            action
                    );
                    processTask(bcastTask, immediate, telescope);
                }
            } catch (PayloadGenerationException e) {
                e.printStackTrace();
            }
        }
        Map<String, ItemOnDemandHierarchy> onDemands =
                hierarchyExpander.onDemandHierarchiesFor((Item) content);
        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
            try {
                ItemOnDemandHierarchy onDemandHierarchy = onDemand.getValue();
                //If this has multiple locations, they should all be the same in terms of available.
                Location location = onDemandHierarchy.locations().get(0);
                action = location.getAvailable() ? Action.UPDATE : Action.DELETE;

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
            } catch (PayloadGenerationException e) {
                e.printStackTrace();
            }
        }

        if (immediate) {
            Item item = (Item) content;
            resolveAndUploadParent(hierarchyExpander, item.getContainer(), true, telescope);

            if (item instanceof Episode) {
                Episode episode = (Episode) item;
                resolveAndUploadParent(hierarchyExpander, episode.getSeriesRef(), true, telescope);
            }
        }
    }

    private Set<Content> extractForDeletion(Content mergedContent) {
        Set<String> contributingUris = getContributingAsins(mergedContent);

        ResolvedContent resolved = contentResolver.findByUris(contributingUris);
        if (resolved != null && resolved.getAllResolvedResults() != null) {
            return resolved.getAllResolvedResults().stream()
                    .filter(c -> c instanceof Content)
                    .map(c -> (Content) c)
                    .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    private Map<Action, Item> getActionsToProcess(Item item, Action action) {
        Map<Action, Item> actionsToProcess = Maps.newHashMap();

        if(Publisher.AMAZON_UNBOX.equals(item.getPublisher())) {
            if (action.equals(Action.DELETE)) {
                actionsToProcess.put(Action.DELETE, getWithAllQualities(item));
            }
            else if (action.equals(Action.UPDATE)) {
                actionsToProcess.put(Action.UPDATE, item);
                actionsToProcess.put(Action.DELETE, getWithStaleQualities(item.copy()));
            }
        } else {
            actionsToProcess.put(action, item);
        }

        return actionsToProcess;
    }

    private Item getWithAllQualities(@Nonnull Item item) {
        //there should only be one, because when we are deleting this, it is non merged
        java.util.Optional<Version> versionOpt = item.getVersions()
                .stream()
                .findFirst();

        if (versionOpt.isPresent()) {
            Version version = versionOpt.get();
            item.removeVersion(version);

            //as above, only 1
            java.util.Optional<Encoding> encodingOpt = version.getManifestedAs()
                    .stream()
                    .findFirst();
            if (encodingOpt.isPresent()) {
                Encoding encoding = encodingOpt.get();
                encoding.getAvailableAt().forEach(location -> location.setAvailable(false));

                //try to add all other qualities. The ASINs will be wrong, but for deletes
                //it should not matter as they are based on the imi only
                for (Quality quality : Quality.values()) {
                    if (quality != encoding.getQuality()) {
                        Encoding copy = encoding.copy();
                        copy.setQuality(quality);
                        version.addManifestedAs(copy);
                    }
                }
            }
            item.addVersion(version);

            return item;
        }
        return item;
    }

    private Item getWithStaleQualities(Item item) {
        //there should only be one Version
        java.util.Optional<Version> versionOpt = item.getVersions().stream().findFirst();
        if (versionOpt.isPresent()) {
            Version version = versionOpt.get();

            // remove the version from item.
            item.removeVersion(version);

            java.util.Optional<Encoding> encodingTemplateOpt =
                    version.getManifestedAs().stream().findFirst();
            if (encodingTemplateOpt.isPresent()) {
                Encoding encodingTemplate = encodingTemplateOpt.get();
                encodingTemplate.getAvailableAt().forEach(location -> location.setAvailable(false));

                Set<Quality> existingQualities = version.getManifestedAs().stream()
                        .map(Encoding::getQuality)
                        .collect(Collectors.toSet());

                Set<Encoding> staleEncodings = Sets.newHashSet();
                for (Quality quality : Quality.values()) {
                    if (!existingQualities.contains(quality)) {
                        Encoding staleEncoding = encodingTemplate.copy();
                        staleEncoding.setQuality(quality);
                        staleEncodings.add(staleEncoding);
                    }
                }

                version.setManifestedAs(staleEncodings);

                item.addVersion(version);
            }
        }

        return item;
    }

    private void uploadChannel(
            boolean immediate,
            IdGenerator idGenerator,
            Channel channel,
            boolean masterbrand,
            FeedsTelescopeReporter telescope)

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

    private void resolveAndUploadParent(
            ContentHierarchyExpander hierarchyExpander,
            ParentRef ref,
            boolean immediate,
            FeedsTelescopeReporter telescope
    ) throws PayloadGenerationException {

        if (ref == null) {
            return;
        }

        java.util.Optional<Content> series = getMergedContent(ref.getUri());
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
            taskProcessor.process(savedTask, telescope);
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
    public void deleteContent(
            HttpServletResponse response,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri") String uri,
            @RequestParam(value = "element_id") String elementId,
            @RequestParam(value = "type") String typeStr
    ) throws IOException {

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
        java.util.Optional<Content> optionalContent = getContent(uri);
        if (!optionalContent.isPresent()) {
            sendError(response, SC_BAD_REQUEST, "content does not exist");
            return;
        }
        Content content = optionalContent.get();

        TVAElementType type = parseTypeFrom(typeStr);
        if (type == null) {
            sendError(response, SC_BAD_REQUEST, "Invalid type provided");
            return;
        }

        if (content instanceof Item) {
            OnDemandHierarchyExpander onDemandHierarchyExpander = new OnDemandHierarchyExpander(
                    IdGeneratorFactory.create(content.getPublisher())
            );
            Map<String, ItemOnDemandHierarchy> onDemands =
                    onDemandHierarchyExpander.expandHierarchy((Item) content);

            for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
                Task odTask = taskCreator.deleteFor(onDemand.getKey(), onDemand.getValue());
                taskStore.save(Task.copy(odTask).withManuallyCreated(true).build());
            }
        }

        // TODO ideally this would go via the TaskCreator, but that would require resolving
        // the hierarchies for each type of element
        Destination destination = new YouViewDestination(
                content.getCanonicalUri(),
                type,
                elementId
        );
        Task task = Task.builder()
                .withAction(Action.DELETE)
                .withDestination(destination)
                .withCreated(clock.now())
                .withPublisher(content.getPublisher())
                .withStatus(Status.NEW)
                .withManuallyCreated(true)
                .build();
        taskStore.save(task);

        //If this amazon, we'll get in the trouble of figuring out what the merged content would be
        //and inform our user on the response.
        if (publisher.get().equals(Publisher.AMAZON_UNBOX)) {
            YouviewContentMerger merger = new YouviewContentMerger(
                    mergingResolver,
                    Publisher.AMAZON_UNBOX
            );
            Content merged = merger.equivAndMerge(content);
            AmazonContentConsolidator.consolidate(merged); //mutates the item

            Set<String> contributingUris = getContributingUris(repId, merged);
            sendOkResponse(
                    response,
                    new StringBuilder().append("Delete for ")
                            .append(content.getCanonicalUri())
                            .append(" sent successfully.<br>")
                            .append("This amazon content is represented by ")
                            .append(merged.getCanonicalUri())
                            .append("<br>")
                            .append("The complete set of contributing ids is ")
                            .append(contributingUris)
                            .toString()
            );
        } else {
            sendOkResponse(response, "Delete for " + uri + " sent successfully ");
        }
    }

    /**
     * Revokes a particular piece of content from the YouView system.
     *
     * @param uri the canonical uri for the item to revoke
     * @throws IOException
     */
    @RequestMapping(value = "/feeds/youview/{publisher}/revoke", method = RequestMethod.POST)
    public void revokeContent(HttpServletResponse response, HttpServletRequest request,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {

        FeedsTelescopeReporter telescope = FeedsTelescopeReporterFactory.getInstance()
                .getTelescopeReporter(FeedsReporterNames.YOU_VIEW_REVOKER);
        telescope.startReporting();

        try {
            Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
            if (!publisher.isPresent()) {
                telescope.reportFailedEvent(
                        "The call to " + request.getRequestURL()
                        + " failed, because publisher "+ publisherStr+" was not found.");
                telescope.endReporting();
                sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
                return;
            }
            if (uri == null) {
                throw new IllegalArgumentException("Required parameter 'uri' not specified");
            }
            java.util.Optional<Content> toBeRevoked = getMergedContent(uri);
            if (!toBeRevoked.isPresent()) {
                throw new IllegalArgumentException( "Content does not exist");
            }

            ImmutableList<Task> revocationTasks = revocationProcessor.revoke(toBeRevoked.get());
            for (Task revocationTask : revocationTasks) {
                processTask(revocationTask, true, telescope);
            }

            sendOkResponse(response, "Revoke for " + uri + " sent successfully");
        } catch(Exception e) {
            telescope.reportFailedEvent(
                "The call to " + request.getRequestURL()
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
    public void unrevokeContent(HttpServletResponse response, HttpServletRequest request,
            @PathVariable("publisher") String publisherStr,
            @RequestParam(value = "uri", required = true) String uri) throws IOException {

        FeedsTelescopeReporter telescope = FeedsTelescopeReporterFactory.getInstance()
                .getTelescopeReporter(FeedsReporterNames.YOU_VIEW_UNREVOKER);
        telescope.startReporting();

        try {
            Optional<Publisher> publisher = findPublisher(publisherStr.trim().toUpperCase());
            if (!publisher.isPresent()) {
                telescope.reportFailedEvent(
                    "The call to " + request.getRequestURL()
                    + " failed because publisher "+ publisherStr+" was not found.");
                telescope.endReporting();
                sendError(response, SC_NOT_FOUND, "Publisher " + publisherStr + " not found.");
                return;
            }
            if (uri == null) {
                throw new IllegalArgumentException("Required parameter 'uri' not specified");
            }
            java.util.Optional<Content> toBeUnrevoked = getMergedContent(uri);
            if (!toBeUnrevoked.isPresent()) {
               throw new IllegalArgumentException ("Content does not exist");
            }

            ImmutableList<Task> revocationTasks = revocationProcessor.unrevoke(toBeUnrevoked.get());
            for (Task revocationTask : revocationTasks) {
                processTask(revocationTask, true, telescope);
            }

            sendOkResponse(response, "Unrevoke for " + uri + " sent sucessfully");
        } catch(Exception e) {
            telescope.reportFailedEvent(
                    "The call to " + request.getRequestURL() + " failed. (" + e.toString() + ")");
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
    }

    private java.util.Optional<Content> getContent(String contentUri) {
        ResolvedContent resolvedContent =
                contentResolver.findByCanonicalUris(ImmutableList.of(contentUri));

        Content content = (Content) resolvedContent.getFirstValue().valueOrNull();
        return java.util.Optional.ofNullable(content);
    }

    /**
     * This method will pass amazon content through equivAndMerge. Consequently the uploads will
     * not upload the Uri that was given, but rather follow the normal upload procedure for
     * amazon, find the equivs for the given uri, merge the content into one and upload it under
     * the repId.
     * @param contentUri
     * @return
     */
    private java.util.Optional<Content> getMergedContent(String contentUri) {
        ResolvedContent resolvedContent =
                contentResolver.findByCanonicalUris(ImmutableList.of(contentUri));

        Content content = (Content) resolvedContent.getFirstValue().valueOrNull();
        if (content != null && content.getPublisher().equals(Publisher.AMAZON_UNBOX)) {
            YouviewContentMerger merger = new YouviewContentMerger(
                    mergingResolver,
                    content.getPublisher()
            );
            Content merged = merger.equivAndMerge(content);
            AmazonContentConsolidator.consolidate(merged); //mutates the item
            return java.util.Optional.of(merged);
        } else if (content != null) {
            return java.util.Optional.of(content);
        }
        return java.util.Optional.empty();
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
            if(isException()) {
                return exception.get();
            } else {
                return null;
            }
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
        private RevocationProcessor revocationProcessor;
        private Clock clock;
        private TaskProcessor taskProcessor;
        private ScheduleResolver scheduleResolver;
        private ChannelResolver channelResolver;

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

        public YouViewUploadController build() {
            return new YouViewUploadController(
                    contentResolver,
                    taskCreator,
                    taskStore,
                    payloadCreator,
                    revocationProcessor,
                    taskProcessor,
                    scheduleResolver,
                    channelResolver,
                    clock
            );
        }
    }
}
