package org.atlasapi.feeds.tasks.youview.creation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.atlasapi.feeds.RepIdClientFactory;
import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.FilterFactory;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.HashType;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelType;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Quality;
import org.atlasapi.media.entity.Version;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.representative.client.RepIdClientWithApp;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mongodb.WriteResult;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;


public abstract class TaskCreationTask extends ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(TaskCreationTask.class);

    private final HashCheck hashCheckMode;
    private final YouViewLastUpdatedStore lastUpdatedStore;
    private final YouViewPayloadHashStore payloadHashStore;
    private final Publisher publisher;
    private final ContentHierarchyExpander hierarchyExpander;
    private final IdGenerator idGenerator;
    private final TaskStore taskStore;
    private final TaskCreator taskCreator;
    private final PayloadCreator payloadCreator;
    private final RepIdClientWithApp repIdClient;

    public TaskCreationTask(
            YouViewLastUpdatedStore lastUpdatedStore,
            Publisher publisher,
            ContentHierarchyExpander hierarchyExpander,
            IdGenerator idGenerator,
            TaskStore taskStore,
            TaskCreator taskCreator,
            PayloadCreator payloadCreator,
            YouViewPayloadHashStore payloadHashStore) {
        this(lastUpdatedStore, publisher, hierarchyExpander, idGenerator, taskStore, taskCreator,
                payloadCreator, payloadHashStore, HashCheck.CHECK
        );
    }

    public TaskCreationTask(
            YouViewLastUpdatedStore lastUpdatedStore,
            Publisher publisher,
            ContentHierarchyExpander hierarchyExpander,
            IdGenerator idGenerator,
            TaskStore taskStore,
            TaskCreator taskCreator,
            PayloadCreator payloadCreator,
            YouViewPayloadHashStore payloadHashStore,
            HashCheck hashCheckMode
    ) {
        this.lastUpdatedStore = checkNotNull(lastUpdatedStore);
        this.publisher = checkNotNull(publisher);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
        this.idGenerator = checkNotNull(idGenerator);
        this.taskStore = checkNotNull(taskStore);
        this.taskCreator = checkNotNull(taskCreator);
        this.payloadCreator = checkNotNull(payloadCreator);
        this.payloadHashStore = checkNotNull(payloadHashStore);
        this.hashCheckMode = hashCheckMode;
        this.repIdClient = RepIdClientFactory.getRepIdClient(publisher);
    }

    protected Publisher getPublisher(){
        return publisher;
    }

    protected RepIdClientWithApp getRepIdClient() {
        return repIdClient;
    }

    protected Optional<DateTime> getLastUpdatedTime() {
        return lastUpdatedStore.getLastUpdated(publisher);
    }

    protected Optional<DateTime> getLastRepIdChangesChecked() {
        return lastUpdatedStore.getLastRepIdChangesChecked(publisher);
    }

    protected void setLastUpdatedTime(DateTime lastUpdated) {
        lastUpdatedStore.setLastUpdated(lastUpdated, publisher);
    }

    protected void setLastRepIdChecked(DateTime lastUpdated) {
        lastUpdatedStore.setLastRepIdChecked(lastUpdated, publisher);
    }

    protected boolean isActivelyPublished(Content content) {
        return content.isActivelyPublished();
    }

    // TODO write last updated time every n items
    protected YouViewContentProcessor contentProcessor(final DateTime updatedSince, final Action action) {
        return new YouViewContentProcessor() {

            UpdateProgress progress = UpdateProgress.START;
            @Override
            public boolean process(Content content) {
                try {
                    if (content instanceof Item) {
                        progress = progress.reduce(processVersions((Item) content, updatedSince, action));
                    }
                    progress = progress.reduce(processContent(content, action));
                } catch (Exception e) {
                    log.error("error on upload for " + content.getCanonicalUri(), e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                return shouldContinue();
            }

            @Override
            public UpdateProgress getResult() {
                return progress;
            }
        };
    }

    protected YouViewChannelProcessor channelProcessor(final Action action, final ChannelType channelType) {
        return new YouViewChannelProcessor() {

            UpdateProgress progress = UpdateProgress.START;

            @Override
            public boolean process(Channel content) {
                try {
                    progress = progress.reduce(processChannel(content, action, channelType));
                } catch (Exception e) {
                    log.error("error on upload for " + content.getCanonicalUri(), e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                return shouldContinue();
            }

            @Override
            public UpdateProgress getResult() {
                return progress;
            }
        };
    }

    protected UpdateProgress processVersions(Item item, DateTime updatedSince, Action action) {
        Map<Action, Item> actionsToProcess = amazonHackaround(item, action);

        UpdateProgress progress = UpdateProgress.START;

        for (Entry<Action, Item> actionToProcess : actionsToProcess.entrySet()) {
            item = actionToProcess.getValue();
            action = actionToProcess.getKey();
            processActions(item, updatedSince, action, progress);
        }

        return progress;
    }

    private UpdateProgress processActions(
            Item item,
            DateTime updatedSince,
            Action action,
            UpdateProgress progress
    ) {
        Map<String, ItemAndVersion> versionHierarchies = hierarchyExpander.versionHierarchiesFor(item);
        Map<String, ItemBroadcastHierarchy> broadcastHierarchies = hierarchyExpander.broadcastHierarchiesFor(item);
        Map<String, ItemOnDemandHierarchy> onDemandHierarchies = hierarchyExpander.onDemandHierarchiesFor(item);

        if (action.equals(Action.UPDATE)) {
            versionHierarchies = Maps.filterValues(
                    versionHierarchies,
                    FilterFactory.versionFilter(updatedSince)
            );
            broadcastHierarchies = Maps.filterValues(
                    broadcastHierarchies,
                    FilterFactory.broadcastFilter(updatedSince)
            );
            onDemandHierarchies = Maps.filterValues(
                    onDemandHierarchies,
                    FilterFactory.onDemandFilter(updatedSince)
            );
        }

        for (Entry<String, ItemAndVersion> version : versionHierarchies.entrySet()) {
            progress = progress.reduce(processVersion(
                    version.getKey(),
                    version.getValue(),
                    action
            ));
        }
        for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcastHierarchies.entrySet()) {
            progress = progress.reduce(processBroadcast(
                    broadcast.getKey(),
                    broadcast.getValue(),
                    action
            ));
        }
        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemandHierarchies.entrySet()) {
            progress = progress.reduce(processOnDemand(
                    onDemand.getKey(),
                    onDemand.getValue(),
                    action)
            );
        }
        return progress;
    }

    // So lets say that there is an equiv graph of MBIDs a - b.
    // When "a" was the rep id of the graph, you have uploaded ondemands
    // a:SD and a:HD. "a", which was the SD version, got unpublished. You are now trying to
    // delete a:SD, but who is going to delete a:HD? Nobody is going to delete a:HD!
    // When b:HD is being uploaded it is no longer aware that "a" even existed, so it cannot
    // delete its predecessors (i.e. a:HD). Similarly at the point that we are deleting a:SD,
    // we don't know if "a" has been uploaded with other qualities as well.
    // So, to solve this, we are doing to delete all of them *insert evil emoticon*
    // This is safe, because if "a" is being unpublished, then all relevant qualities that
    // "a" used to represent should be unpublished, and it is also safe, because if we
    // haven't uploaded some of them, the hashstore will prevent deletes from being sent to
    // YV. The question is how to unpublish all qualities, and this is where this sad
    // hack comes in.
    private Map<Action, Item> amazonHackaround(Item item, Action action) {
        Map<Action, Item> actionsToProcess = Maps.newHashMap();

        if (action.equals(Action.DELETE) && Publisher.AMAZON_UNBOX.equals(item.getPublisher())) {
            triggerDeleteForAllQualitiesUnderRepId(item, actionsToProcess);
        }

        // from example above, say b is unpublished. b:HD will be deleted, but that doesn't exists.
        // because the equiv set changed, it'll trigger update for the rest of the IDs from its
        // previous equiv set. so, when a is updated, we need to delete the qualities that are not
        // present on the item at that point, in this case a:HD
        if (action.equals(Action.UPDATE) && Publisher.AMAZON_UNBOX.equals(item.getPublisher())) {
            // initial update which needs to be processed anyway
            actionsToProcess.put(Action.UPDATE, item);

            triggerDeleteForStaleQualitiesUnderRepId(item, actionsToProcess);
        }

        return actionsToProcess;
    }

    private void triggerDeleteForAllQualitiesUnderRepId(
            Item item,
            Map<Action, Item> actionsToProcess
    ) {
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

            actionsToProcess.put(Action.DELETE, item);
        }
    }

    private void triggerDeleteForStaleQualitiesUnderRepId(
            Item item,
            Map<Action, Item> actionsToProcess
    ) {
        //create new item with qualities to delete
        Item itemWithQualitiesToDelete = item.copy();

        Set<Quality> qualitiesOnItem = new HashSet<>();
        for (Version version : item.getVersions()) {
            // delete the current versions that will be uploaded through item
            itemWithQualitiesToDelete.removeVersion(version);
            for (Encoding encoding : version.getManifestedAs()) {
                qualitiesOnItem.add(encoding.getQuality());
            }
        }

        // get copies of Version and Encoding for itemWithQualitiesToDelete
        Version version = item.getVersions()
                .stream()
                .findFirst()
                .get();
        Encoding encoding = version
                .getManifestedAs()
                .stream()
                .findFirst()
                .get();
        encoding.getAvailableAt().forEach(location -> location.setAvailable(false));

        //create Version and Encoding for each quality not present on the item
        for (Quality quality : Quality.values()) {
            if (!qualitiesOnItem.contains(quality)) {
                Encoding newEncoding = encoding.copy();
                newEncoding.setQuality(quality);

                Version newVersion = version.copy();
                newVersion.setManifestedAs(ImmutableSet.of(encoding));

                itemWithQualitiesToDelete.addVersion(newVersion);
            }
        }

        actionsToProcess.put(Action.DELETE, itemWithQualitiesToDelete);
    }

    private UpdateProgress processContent(Content content, Action action) {
        String contentCrid = idGenerator.generateContentCrid(content);
        log.debug("Processing Content {}", contentCrid);
        try {
            if (Publisher.AMAZON_UNBOX.equals(content.getPublisher())) {
                HashType hashType = action == Action.UPDATE ? HashType.CONTENT : HashType.DELETE;
                Payload p = payloadCreator.payloadFrom(contentCrid, content);
                if (shouldSave(hashType, contentCrid, p)) {
                    save(payloadHashStore, taskStore, taskCreator.taskFor(contentCrid, content, p, action));
                }
            } else {
                // not strictly necessary, but will save space
                if (!Action.DELETE.equals(action)) {
                    Payload p = payloadCreator.payloadFrom(contentCrid, content);

                    if (shouldSave(HashType.CONTENT, contentCrid, p)) {
                        taskStore.save(taskCreator.taskFor(contentCrid, content, p, action));
                        payloadHashStore.saveHash(HashType.CONTENT, contentCrid, p.hash());
                    } else {
                        log.debug("Existing hash found for Content {}, not updating", contentCrid);
                    }
                }
            }
            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error("Failed to create payload for content {}", content.getCanonicalUri(), e);
            Task task = taskStore.save(taskCreator.taskFor(idGenerator.generateContentCrid(content), content, action, Status.FAILED));
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
            return UpdateProgress.FAILURE;
        }
    }

    private UpdateProgress processChannel(Channel channel, Action action, ChannelType channelType) {
        String channelCrid = idGenerator.generateChannelCrid(channel);
        log.debug("Processing Channel {}", channelCrid);
        try {
            // not strictly necessary, but will save space
            if (!Action.DELETE.equals(action)) {

                Payload p = payloadCreator.payloadFrom(
                        channel,
                        channelType == ChannelType.MASTERBRAND
                );

                if (shouldSave(HashType.CHANNEL, channelCrid, p)) {
                    taskStore.save(taskCreator.taskFor(channelCrid, channel, p, action));
                    payloadHashStore.saveHash(HashType.CHANNEL, channelCrid, p.hash());
                } else {
                    log.debug("Existing hash found for Channel {}, not updating", channelCrid);
                }
            }

            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error("Failed to create payload for channel {}", channel.getCanonicalUri(), e);
            Task task = taskStore.save(taskCreator.taskFor(channelCrid, channel, action, Status.FAILED));
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
            return UpdateProgress.FAILURE;
        }
    }

    private UpdateProgress processVersion(
            String versionCrid,
            ItemAndVersion versionHierarchy,
            Action action
    ) {
        try {
            log.debug("Processing Version {}", versionCrid);

            Payload payload = payloadCreator.payloadFrom(versionCrid, versionHierarchy);
            Task task = taskCreator.taskFor(
                    versionCrid,
                    versionHierarchy,
                    payload,
                    action
            );

            if (Publisher.AMAZON_UNBOX.equals(versionHierarchy.item().getPublisher())) {
                HashType hashType = action == Action.UPDATE ? HashType.VERSION : HashType.DELETE;
                if (shouldSave(hashType, versionCrid, payload)) {
                    save(payloadHashStore, taskStore, task);
                }
            } else {
                if (shouldSave(HashType.VERSION, versionCrid, payload)) {
                    Task savedTask = taskStore.save(task);
                    payloadHashStore.saveHash(HashType.VERSION, versionCrid, payload.hash());
                    log.debug(
                            "Saved task {} for version {} with hash {}",
                            savedTask.id(),
                            versionCrid,
                            payload.hash()
                    );
                } else {
                    log.debug("Existing hash found for Version {}, not updating", versionCrid);
                }
            }
            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error(String.format(
                            "Failed to create payload for content %s, version %s",
                            versionHierarchy.item().getCanonicalUri(),
                            versionHierarchy.version().getCanonicalUri()
                    ),
                    e
            );
            Task task = taskStore.save(
                    taskCreator.taskFor(versionCrid, versionHierarchy, action, Status.FAILED)
            );
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
            return UpdateProgress.FAILURE;
        }
    }

    private UpdateProgress processBroadcast(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action) {
        try {
            log.debug("Processing Broadcast {}", broadcastImi);

            //This might not return a payload based a different mechanism. Getting an empty
            //payload here is part of the logic.
            Optional<Payload> payload = payloadCreator.payloadFrom(broadcastImi, broadcastHierarchy);
            if (!payload.isPresent()) {
                return UpdateProgress.START;
            }

            if (shouldSave(HashType.BROADCAST, broadcastImi, payload.get())) {
                Task unsavedTask = taskCreator.taskFor(broadcastImi, broadcastHierarchy, payload.get(), action);
                taskStore.save(unsavedTask);
                payloadHashStore.saveHash(HashType.BROADCAST, broadcastImi, payload.get().hash());
            } else {
                log.debug("Existing hash found for Broadcast {}, not updating", broadcastImi);
            }

            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error(String.format(
                            "Failed to create payload for content %s, version %s, broadcast %s",
                            broadcastHierarchy.item().getCanonicalUri(),
                            broadcastHierarchy.version().getCanonicalUri(),
                            broadcastHierarchy.broadcast().toString()
                    ),
                    e
            );
            Task task = taskStore.save(taskCreator.taskFor(broadcastImi, broadcastHierarchy, action, Status.FAILED));
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
            return UpdateProgress.FAILURE;
        }
    }

    private String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return e.getMessage() + " " + sw.toString();
    }

    private UpdateProgress processOnDemand(
            String onDemandImi,
            ItemOnDemandHierarchy onDemandHierarchy,
            Action action) {

        if (Publisher.AMAZON_UNBOX.equals(onDemandHierarchy.item().getPublisher())) {
            if(action.equals(Action.UPDATE)){  //if it is delete, always delete.
                //Amazon's locations should all be the same
                Location location = onDemandHierarchy.locations().get(0);
                action = location.getAvailable() ? Action.UPDATE : Action.DELETE;
            }
        } else {
            //Nitro only has 1 location.
            Location location = onDemandHierarchy.locations().get(0);
            action = location.getAvailable() ? Action.UPDATE : Action.DELETE;
        }

        HashType hashType = action == Action.UPDATE ? HashType.ON_DEMAND : HashType.DELETE;

        try {
            log.debug("Processing OnDemand {}", onDemandImi);

            Payload p = payloadCreator.payloadFrom(onDemandImi, onDemandHierarchy);

            if (shouldSave(hashType, onDemandImi, p)) {
                if (Publisher.AMAZON_UNBOX.equals(onDemandHierarchy.item().getPublisher())) {
                    save(payloadHashStore, taskStore, taskCreator.taskFor(
                            onDemandImi,
                            onDemandHierarchy,
                            p,
                            action
                    ));
                } else {

                    taskStore.save(taskCreator.taskFor(
                            onDemandImi,
                            onDemandHierarchy,
                            p,
                            action
                    ));
                    payloadHashStore.saveHash(hashType, onDemandImi, p.hash());
                }
            } else {
                log.debug("Existing hash found for OnDemand {}, not updating", onDemandImi);
            }

            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error(
                    "Failed to create payload for content {}, version {}, encoding {}, locations {}",
                    onDemandHierarchy.item().getCanonicalUri(),
                    onDemandHierarchy.version().getCanonicalUri(),
                    onDemandHierarchy.encoding().toString(),
                    onDemandHierarchy.locations().get(0).toString(),
                    e
            );
            Task task = taskStore.save(taskCreator.taskFor(
                    onDemandImi,
                    onDemandHierarchy,
                    action,
                    Status.FAILED
            ));
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
            return UpdateProgress.FAILURE;
        }
    }

    //Returns null if nothing was saved. Throws an exception if nothing could be saved.
    public static Task save(
            YouViewPayloadHashStore payloadHashStore,
            TaskStore taskStore,
            Task task
    ) throws IllegalArgumentException {
        try {
            YouViewDestination destination = (YouViewDestination) task.destination();
            HashType hashType = matchHashType(destination.elementType());

            if (Action.UPDATE.equals(task.action())) {
                //if this an update, we need to remove the delete hash (if any)
                payloadHashStore.removeHash(HashType.DELETE, destination.elementId());
                Task savedTask = taskStore.save(task);
                Optional<String> hash = task.payload().transform(Payload::hash);
                payloadHashStore.saveHash( hashType, destination.elementId(), hash.get() );
                return savedTask;
            } else {
                //if this is a delete, we need to remove from the db the hash of the upload
                WriteResult writeResult =
                        payloadHashStore.removeHash(hashType, destination.elementId());
                // if there was no update hash, it means that the fragment we are trying to delete
                // was never uploaded, and as such we don't need to delete it.
                // (this can happen by sending proactive delete requests for equived content).
                if (writeResult.getN() > 0) {
                    //otherwise save the delete task, and the delete hash
                    Task savedTask = taskStore.save(task);
                    payloadHashStore.saveHash(HashType.DELETE, destination.elementId(), "");
                    return savedTask;
                }
            }
        } catch (Exception e) { //catch w/e could go wrong to save time.
            throw new IllegalArgumentException("Task could not be saved.", e);
        }

        return null;
    }

    private static HashType matchHashType(TVAElementType elementType) {
        switch (elementType) {
        case BRAND: return HashType.CONTENT;
        case SERIES: return HashType.CONTENT;
        case ITEM: return HashType.CONTENT;
        case VERSION: return HashType.VERSION;
        case ONDEMAND: return HashType.ON_DEMAND;
        case BROADCAST: return HashType.BROADCAST;
        case CHANNEL: return HashType.CHANNEL;
        default:
            throw new IllegalArgumentException("YV Element type ("
                                               + elementType
                                               + ") does not correspond to a valid HashType.");
        }
    }

    private boolean shouldSave(HashType type, String id, Payload payload) {
        java.util.Optional<String> storedHash = payloadHashStore.getHash(type, id);

        if (type == HashType.DELETE) {
            return !storedHash.isPresent();
        } else {
            return (hashCheckMode == HashCheck.IGNORE || !storedHash.isPresent())
                    || (hashCheckMode == HashCheck.CHECK && payload.hasChanged(storedHash.get()));
        }
    }

    protected enum HashCheck {
        CHECK,
        IGNORE
    }
}
