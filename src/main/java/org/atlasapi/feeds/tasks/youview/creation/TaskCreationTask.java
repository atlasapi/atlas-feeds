package org.atlasapi.feeds.tasks.youview.creation;

import static com.google.common.base.Preconditions.checkNotNull;

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
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;


public abstract class TaskCreationTask extends ScheduledTask {
    
    private final Logger log = LoggerFactory.getLogger(TaskCreationTask.class);

    private final HashCheck hashCheckMode;
    private final YouViewLastUpdatedStore lastUpdatedStore;
    private final YouViewPayloadHashStore payloadHashStore;
    private final Publisher publisher;
    private final ContentHierarchyExpander hierarchyExpander;
    private final IdGenerator idGenerator;
    private final TaskStore taskStore;
    private final TaskCreator taskCreator;
    private final PayloadCreator payloadCreator;

    public TaskCreationTask(YouViewLastUpdatedStore lastUpdatedStore,
            Publisher publisher, ContentHierarchyExpander hierarchyExpander,
            IdGenerator idGenerator, TaskStore taskStore, TaskCreator taskCreator,
            PayloadCreator payloadCreator, YouViewPayloadHashStore payloadHashStore) {
        this(lastUpdatedStore, publisher, hierarchyExpander, idGenerator, taskStore, taskCreator,
                payloadCreator, payloadHashStore, HashCheck.CHECK);
    }

    public TaskCreationTask(YouViewLastUpdatedStore lastUpdatedStore,
            Publisher publisher, ContentHierarchyExpander hierarchyExpander,
            IdGenerator idGenerator, TaskStore taskStore, TaskCreator taskCreator,
            PayloadCreator payloadCreator, YouViewPayloadHashStore payloadHashStore,
            HashCheck hashCheckMode) {
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
    
    // TODO tidy this up, ideally simplify/streamline it
    private UpdateProgress processVersions(Item item, DateTime updatedSince, Action action) {
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
        
        UpdateProgress progress = UpdateProgress.START;
        
        for (Entry<String, ItemAndVersion> version : versionHierarchies.entrySet()) {
            progress = progress.reduce(processVersion(version.getKey(), version.getValue(), action));
        }
        for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcastHierarchies.entrySet()) {
            progress = progress.reduce(processBroadcast(broadcast.getKey(), broadcast.getValue(), action));
        }
        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemandHierarchies.entrySet()) {
            progress = progress.reduce(processOnDemand(onDemand.getKey(), onDemand.getValue(), action));
        }
        return progress;
    }

    private UpdateProgress processContent(Content content, Action action) {
        String contentCrid = idGenerator.generateContentCrid(content);
        try {
            // not strictly necessary, but will save space
            if (!Action.DELETE.equals(action)) {
                Payload p = payloadCreator.payloadFrom(contentCrid, content);

                if (shouldSave(HashType.CONTENT, contentCrid, p)) {
                    Task task = taskStore.save(taskCreator.taskFor(contentCrid, content, action));
                    taskStore.updateWithPayload(task.id(), p);
                    payloadHashStore.saveHash(HashType.CONTENT, contentCrid, p.hash());
                }
            }

            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error("Failed to create payload for content {}", content.getCanonicalUri(), e);
            Task task = taskStore.save(taskCreator.taskFor(contentCrid, content, action));
            taskStore.updateWithStatus(task.id(), Status.FAILED);
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
            return UpdateProgress.FAILURE;
        }
    }
    
    private UpdateProgress processVersion(String versionCrid, ItemAndVersion versionHierarchy, Action action) {
        try {
            Payload payload = payloadCreator.payloadFrom(versionCrid, versionHierarchy);

            if (shouldSave(HashType.VERSION, versionCrid, payload)) {
                Task task = taskStore.save(taskCreator.taskFor(versionCrid, versionHierarchy, action));
                taskStore.updateWithPayload(task.id(), payload);
                payloadHashStore.saveHash(HashType.VERSION, versionCrid, payload.hash());
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
            Task task = taskStore.save(taskCreator.taskFor(versionCrid, versionHierarchy, action));
            taskStore.updateWithStatus(task.id(), Status.FAILED);
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
            return UpdateProgress.FAILURE;
        }
    }

    private UpdateProgress processBroadcast(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action) {
        try {
            Optional<Payload> p = payloadCreator.payloadFrom(broadcastImi, broadcastHierarchy);
            if (!p.isPresent()) {
                return UpdateProgress.START;
            }

            if (shouldSave(HashType.BROADCAST, broadcastImi, p.get())) {
                Task task = taskStore.save(taskCreator.taskFor(broadcastImi, broadcastHierarchy, action));
                taskStore.updateWithPayload(task.id(), p.get());
                payloadHashStore.saveHash(HashType.BROADCAST, broadcastImi, p.get().hash());
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
    
    private UpdateProgress processOnDemand(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Action action) {
        try {
            Payload p = payloadCreator.payloadFrom(onDemandImi, onDemandHierarchy);

            if (shouldSave(HashType.ON_DEMAND, onDemandImi, p)) {
                Task task = taskStore.save(taskCreator.taskFor(onDemandImi, onDemandHierarchy, action));
                taskStore.updateWithPayload(task.id(), p);
                payloadHashStore.saveHash(HashType.ON_DEMAND, onDemandImi, p.hash());
            }

            return UpdateProgress.SUCCESS;
        } catch (Exception e) {
            log.error(String.format(
                            "Failed to create payload for content %s, version %s, encoding %s, location %s", 
                            onDemandHierarchy.item().getCanonicalUri(),
                            onDemandHierarchy.version().getCanonicalUri(),
                            onDemandHierarchy.encoding().toString(),
                            onDemandHierarchy.location().toString()
                    ),
                    e
            );
            Task task = taskStore.save(taskCreator.taskFor(onDemandImi, onDemandHierarchy, action));
            taskStore.updateWithStatus(task.id(), Status.FAILED);
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
            return UpdateProgress.FAILURE;
        }
    }

    private boolean shouldSave(HashType type, String imi, Payload payload) {
        Optional<String> hash = payloadHashStore.getHash(type, imi);
        return (hashCheckMode == HashCheck.IGNORE || !hash.isPresent())
                || (hashCheckMode == HashCheck.CHECK && payload.hasChanged(hash.get()));
    }

    protected enum HashCheck {
        CHECK,
        IGNORE
    }
}
