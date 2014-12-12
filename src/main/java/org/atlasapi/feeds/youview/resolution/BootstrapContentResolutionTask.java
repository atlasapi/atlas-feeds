package org.atlasapi.feeds.youview.resolution;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.creation.TaskCreator;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metabroadcast.common.scheduling.ScheduledTask;


public class BootstrapContentResolutionTask extends ScheduledTask {

    private final Logger log = LoggerFactory.getLogger(BootstrapContentResolutionTask.class);
    
    private final ContentHierarchyExpander hierarchyExpander;
    private final RevokedContentStore revocationStore;
    private final YouViewContentResolver contentResolver;
    private final TaskCreator taskCreator;
  
    public BootstrapContentResolutionTask(ContentHierarchyExpander hierarchyExpander, 
            RevokedContentStore revocationStore, YouViewContentResolver contentResolver, 
            TaskCreator taskCreator) {
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
        this.revocationStore = checkNotNull(revocationStore);
        this.contentResolver = checkNotNull(contentResolver);
        this.taskCreator = checkNotNull(taskCreator);
    }

    private Iterator<Content> resolveContent() {
        return contentResolver.allContent();
    }

    // TODO check sent broadcast uri store before creating task for broadcast event
    // TODO record/output counts of items resolved
    @Override
    protected void runTask() {
        Iterator<Content> resolved = resolveContent();
        while (resolved.hasNext()) { 
            Content content = resolved.next();
            if (isRevoked(content)) {
                log.trace("Content {} is revoked, not uploading", content.getCanonicalUri());
                continue;
            }
            expandElementsAndCreateTasks(content);
        }
    }
    
    private boolean isRevoked(Content content) {
        return revocationStore.isRevoked(content.getCanonicalUri());
    }
    
    // TODO consider error handling
    private void expandElementsAndCreateTasks(Content content) {
        
        taskCreator.create(content, Action.UPDATE);
        
        if (content instanceof Item) {
            Map<String, ItemAndVersion> versionHierarchies = hierarchyExpander.versionHierarchiesFor((Item) content);
            Map<String, ItemBroadcastHierarchy> broadcastHierarchies = hierarchyExpander.broadcastHierarchiesFor((Item) content);
            Map<String, ItemOnDemandHierarchy> onDemandHierarchies = hierarchyExpander.onDemandHierarchiesFor((Item) content);
            
            for (Entry<String, ItemAndVersion> version : versionHierarchies.entrySet()) {
                taskCreator.create(version.getKey(), version.getValue(), Action.UPDATE);
            }
            for (Entry<String, ItemBroadcastHierarchy> broadcast : broadcastHierarchies.entrySet()) {
                taskCreator.create(broadcast.getKey(), broadcast.getValue(), Action.UPDATE);
            }
            for (Entry<String, ItemOnDemandHierarchy> onDemand : onDemandHierarchies.entrySet()) {
                taskCreator.create(onDemand.getKey(), onDemand.getValue(), Action.UPDATE);
            }
        }
    }
}
