package org.atlasapi.feeds.tasks.youview.creation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
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
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
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

    public TaskCreationTask(
            YouViewLastUpdatedStore lastUpdatedStore,
            Publisher publisher,
            ContentHierarchyExpander hierarchyExpander,
            IdGenerator idGenerator,
            TaskStore taskStore,
            TaskCreator taskCreator,
            PayloadCreator payloadCreator,
            YouViewPayloadHashStore payloadHashStore
    ) {
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
    }

    //for debugging purposes
    protected String getPublisherString(){
        return publisher.toString();
    }

    protected Optional<DateTime> getLastUpdatedTime() {
        return lastUpdatedStore.getLastUpdated(publisher);
    }

    protected void setLastUpdatedTime(DateTime lastUpdated) {
        lastUpdatedStore.setLastUpdated(lastUpdated, publisher);
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
                    log.info("@@@ CHANNEP PROCESSOR?!?!?!?!?\n\n");
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

    // TODO tidy this up, ideally simplify/streamline it
    private UpdateProgress processVersions(Item item, DateTime updatedSince, Action action) {
        log.info("@@@ Consequently we are processing versions");

        Map<String, ItemAndVersion> versionHierarchies = Maps.filterValues(
                hierarchyExpander.versionHierarchiesFor(item),
                FilterFactory.versionFilter(updatedSince)
        );
        Map<String, ItemBroadcastHierarchy> broadcastHierarchies = Maps.filterValues(
                hierarchyExpander.broadcastHierarchiesFor(item),
                FilterFactory.broadcastFilter(updatedSince)
        );
        Map<String, ItemOnDemandHierarchy> onDemandHierarchies = Maps.filterValues(
                hierarchyExpander.onDemandHierarchiesFor(item),
                FilterFactory.onDemandFilter(updatedSince)
        );

        log.info("@@@ The hierarcy expander has produced "+versionHierarchies.size()+ " versions, "+broadcastHierarchies.size()+" broadcasts, "+onDemandHierarchies.size()+ " onDemands");

        UpdateProgress progress = UpdateProgress.START;

        for (Entry<String, ItemAndVersion> version : versionHierarchies.entrySet()) {
            progress = progress.reduce(processVersion(version.getKey(), version.getValue(), action));
        }
        for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcastHierarchies.entrySet()) {
            progress = progress.reduce(processBroadcast(broadcast.getKey(), broadcast.getValue(), action));
        }
        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemandHierarchies.entrySet()) {
            progress = progress.reduce(processOnDemand(onDemand.getKey(), onDemand.getValue()));
        }
        return progress;
    }

    private UpdateProgress processContent(Content content, Action action) {
        String contentCrid = idGenerator.generateContentCrid(content);
        log.debug("Processing Content {}", contentCrid);
        log.info("@@@ Processing Content {}", contentCrid);
        try {
            // not strictly necessary, but will save space
            if (!Action.DELETE.equals(action)) {
                Payload p = payloadCreator.payloadFrom(contentCrid, content);

                if (shouldSave(HashType.CONTENT, contentCrid, p)) {
                    Task savedTask = taskStore.save(taskCreator.taskFor(idGenerator.generateContentCrid(
                            content), content, p, action));
                    payloadHashStore.saveHash(HashType.CONTENT, contentCrid, p.hash());
                    log.info(
                            "@@@ Saved task {} for content {}",
                            savedTask.id(),
                            contentCrid);
                } else {
                    log.debug("Existing hash found for Content {}, not updating", contentCrid);
                    log.info("@@@ Existing hash found for Content {}, not updating", contentCrid);
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


            log.info("@@@ Processing version {}", versionCrid);
            Payload payload = payloadCreator.payloadFrom(versionCrid, versionHierarchy);
            log.info("@@@ created the payload for the version\n\n"+payload);

            if (shouldSave(HashType.VERSION, versionCrid, payload)) {
                Task savedTask = taskStore.save(taskCreator.taskFor(
                        versionCrid,
                        versionHierarchy,
                        payload,
                        action
                ));
                payloadHashStore.saveHash(HashType.VERSION, versionCrid, payload.hash());
                log.info(
                        "@@@ Saved task {} for version {}",
                        savedTask.id(),
                        versionCrid);
                log.debug(
                        "Saved task {} for version {} with hash {}",
                        savedTask.id(),
                        versionCrid,
                        payload.hash()
                );
            } else {
                log.debug("Existing hash found for Version {}, not updating", versionCrid);
                log.info ("@@@ Existing hash found for Version {}, not updating", versionCrid);
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
            log.info("@@@ Processing Broadcast {}", broadcastImi);

            Optional<Payload> payload = payloadCreator.payloadFrom(broadcastImi, broadcastHierarchy);
            if (!payload.isPresent()) {
                return UpdateProgress.START;
            }

            if (shouldSave(HashType.BROADCAST, broadcastImi, payload.get())) {
                Task unsavedTask = taskCreator.taskFor(broadcastImi, broadcastHierarchy, payload.get(), action);
                Task SaVeDTasK = taskStore.save(unsavedTask);
                payloadHashStore.saveHash(HashType.BROADCAST, broadcastImi, payload.get().hash());
                log.info(
                        "@@@ Saved task {} for broadcast {}",
                        SaVeDTasK.id(),
                        broadcastImi);
            } else {
                log.debug("Existing hash found for Broadcast {}, not updating", broadcastImi);
                log.info("@@@ Existing hash found for Broadcast {}, not updating", broadcastImi);
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
            ItemOnDemandHierarchy onDemandHierarchy
    ) {

        Location location = onDemandHierarchy.location();

        Action action = location.getAvailable() ? Action.UPDATE : Action.DELETE;
        HashType hashType = action == Action.UPDATE ? HashType.ON_DEMAND : HashType.DELETE;

        try {
            log.debug("Processing OnDemand {}", onDemandImi);
            log.debug("@@@ Processing OnDemand {}", onDemandImi);

            Payload p = payloadCreator.payloadFrom(onDemandImi, onDemandHierarchy);

            if (shouldSave(hashType, onDemandImi, p)) {
                Task savedTask = taskStore.save(taskCreator.taskFor(
                        onDemandImi,
                        onDemandHierarchy,
                        p,
                        action
                ));
                payloadHashStore.saveHash(hashType, onDemandImi, p.hash());
                log.info(
                        "@@@ Saved task {} for ondemand {}",
                        savedTask.id(),
                        onDemandImi);
            } else {
                log.debug("Existing hash found for OnDemand {}, not updating", onDemandImi);
                log.info("@@@ Existing hash found for OnDemand {}, not updating", onDemandImi);
            }

            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error(
                    "Failed to create payload for content {}, version {}, encoding {}, location {}",
                    onDemandHierarchy.item().getCanonicalUri(),
                    onDemandHierarchy.version().getCanonicalUri(),
                    onDemandHierarchy.encoding().toString(),
                    location.toString(),
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

    private boolean shouldSave(HashType type, String imi, Payload payload) {
        Optional<String> hash = payloadHashStore.getHash(type, imi);

        if (type == HashType.DELETE) {
            return !hash.isPresent();
        } else {
            return (hashCheckMode == HashCheck.IGNORE || !hash.isPresent())
                    || (hashCheckMode == HashCheck.CHECK && payload.hasChanged(hash.get()));
        }
    }

    protected enum HashCheck {
        CHECK,
        IGNORE
    }
}
