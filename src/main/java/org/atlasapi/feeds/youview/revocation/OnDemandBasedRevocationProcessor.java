package org.atlasapi.feeds.youview.revocation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.creation.TaskCreator;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OnDemandBasedRevocationProcessor implements RevocationProcessor {

    private final Logger log = LoggerFactory.getLogger(OnDemandBasedRevocationProcessor.class);
    
    private final RevokedContentStore revocationStore;
    private final OnDemandHierarchyExpander onDemandHierarchyExpander;
    private final PayloadCreator payloadCreator;
    private final TaskCreator taskCreator;
    private final TaskStore taskStore;
    
    public OnDemandBasedRevocationProcessor(RevokedContentStore revocationStore,
            OnDemandHierarchyExpander onDemandHierarchyExpander, PayloadCreator payloadCreator, 
            TaskCreator taskCreator, TaskStore taskStore) {
        this.revocationStore = checkNotNull(revocationStore);
        this.onDemandHierarchyExpander = checkNotNull(onDemandHierarchyExpander);
        this.payloadCreator = checkNotNull(payloadCreator);
        this.taskCreator = checkNotNull(taskCreator);
        this.taskStore = checkNotNull(taskStore);
    }

    @Override
    public void revoke(Content content) {
        checkArgument(content instanceof Item, "content " + content.getCanonicalUri() + " not an item, cannot revoke");
        
        Item item = (Item) content;
        Map<String, ItemOnDemandHierarchy> onDemands = onDemandHierarchyExpander.expandHierarchy(item);

        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
            taskStore.save(taskCreator.taskFor(onDemand.getKey(), onDemand.getValue(), Action.DELETE));
        }
        revocationStore.revoke(content.getCanonicalUri());
    }

    @Override
    public void unrevoke(Content content) {
        checkArgument(content instanceof Item, "content " + content.getCanonicalUri() + " not an item, cannot unrevoke");
        
        revocationStore.unrevoke(content.getCanonicalUri());
        
        Item item = (Item) content;
        Map<String, ItemOnDemandHierarchy> onDemands = onDemandHierarchyExpander.expandHierarchy(item);
        
        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
            try {
                Task task = taskStore.save(taskCreator.taskFor(onDemand.getKey(), onDemand.getValue(), Action.UPDATE));
                taskStore.updateWithPayload(task.id(), payloadCreator.payloadFrom(onDemand.getKey(), onDemand.getValue()));
            } catch (PayloadGenerationException e) {
                log.error("unable to upload ondemand " + onDemand.getKey());
            }
        }
    }
}
