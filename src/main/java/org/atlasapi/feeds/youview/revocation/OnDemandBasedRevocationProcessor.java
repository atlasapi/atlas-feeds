package org.atlasapi.feeds.youview.revocation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.creation.TaskCreator;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;


public class OnDemandBasedRevocationProcessor implements RevocationProcessor {

    private final RevokedContentStore revocationStore;
    private final OnDemandHierarchyExpander onDemandHierarchyExpander;
    private final TaskCreator taskCreator;
    private final TaskStore taskStore;
    
    public OnDemandBasedRevocationProcessor(RevokedContentStore revocationStore,
            OnDemandHierarchyExpander onDemandHierarchyExpander, TaskCreator taskCreator,
            TaskStore taskStore) {
        this.revocationStore = checkNotNull(revocationStore);
        this.onDemandHierarchyExpander = checkNotNull(onDemandHierarchyExpander);
        this.taskCreator = checkNotNull(taskCreator);
        this.taskStore = checkNotNull(taskStore);
    }

    @Override
    public void revoke(Content content) {
        checkArgument(content instanceof Item, "content " + content.getCanonicalUri() + " not an item, cannot revoke");
        
        Item item = (Item) content;
        Map<String, ItemOnDemandHierarchy> onDemands = onDemandHierarchyExpander.expandHierarchy(item);

        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
            Task task = taskCreator.taskFor(onDemand.getKey(), onDemand.getValue(), Action.DELETE);
            taskStore.save(task);
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
            Task task = taskCreator.taskFor(onDemand.getKey(), onDemand.getValue(), Action.UPDATE);
            taskStore.save(task);
        }
    }
}
