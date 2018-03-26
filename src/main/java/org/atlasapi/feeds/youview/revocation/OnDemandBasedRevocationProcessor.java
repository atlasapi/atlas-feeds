package org.atlasapi.feeds.youview.revocation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.creation.TaskCreator;
import org.atlasapi.feeds.youview.IdGeneratorFactory;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class OnDemandBasedRevocationProcessor implements RevocationProcessor {

    private final Logger log = LoggerFactory.getLogger(OnDemandBasedRevocationProcessor.class);
    
    private final RevokedContentStore revocationStore;
    private final PayloadCreator payloadCreator;
    private final TaskCreator taskCreator;
    private final TaskStore taskStore;
    
    public OnDemandBasedRevocationProcessor(RevokedContentStore revocationStore,
            PayloadCreator payloadCreator,
            TaskCreator taskCreator,
            TaskStore taskStore) {

        this.revocationStore = checkNotNull(revocationStore);
        this.payloadCreator = checkNotNull(payloadCreator);
        this.taskCreator = checkNotNull(taskCreator);
        this.taskStore = checkNotNull(taskStore);
    }

    @Override
    public ImmutableList<Task> revoke(Content content) {
        checkArgument(content instanceof Item, "content " + content.getCanonicalUri() + " not an item, cannot revoke");
        
        Item item = (Item) content;
        OnDemandHierarchyExpander onDemandHierarchyExpander = getOnDemandHierarchyExpander(item);
        Map<String, ItemOnDemandHierarchy> onDemands = onDemandHierarchyExpander.expandHierarchy(item);

        ImmutableList.Builder<Task> taskList = ImmutableList.builder();

        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
            Task task = taskStore.save(taskCreator.deleteFor(onDemand.getKey(), onDemand.getValue()));
            taskList.add(task);
        }
        revocationStore.revoke(content.getCanonicalUri());

        return taskList.build();
    }

    @Override
    public ImmutableList<Task> unrevoke(Content content) {
        checkArgument(content instanceof Item, "content " + content.getCanonicalUri() + " not an item, cannot unrevoke");
        
        revocationStore.unrevoke(content.getCanonicalUri());
        
        Item item = (Item) content;
        OnDemandHierarchyExpander onDemandHierarchyExpander = getOnDemandHierarchyExpander(item);
        Map<String, ItemOnDemandHierarchy> onDemands = onDemandHierarchyExpander.expandHierarchy(item);

        ImmutableList.Builder<Task> taskList = ImmutableList.builder();

        for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemands.entrySet()) {
            try {
                Payload payload = payloadCreator.payloadFrom(onDemand.getKey(), onDemand.getValue());
                Task task = taskStore.save(taskCreator.taskFor(
                        onDemand.getKey(), onDemand.getValue(),
                        payload, Action.UPDATE
                ));
                taskList.add(task);
            } catch (PayloadGenerationException e) {
                log.error("unable to upload ondemand " + onDemand.getKey());
            }
        }

        return taskList.build();
    }

    protected OnDemandHierarchyExpander getOnDemandHierarchyExpander(Item item){
        Publisher publisher = item.getPublisher();
        return new OnDemandHierarchyExpander(IdGeneratorFactory.create(publisher));
    }
}
