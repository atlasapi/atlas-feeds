package org.atlasapi.feeds.youview.revocation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;


public class OnDemandBasedRevocationProcessor implements RevocationProcessor {

    private final RevokedContentStore revocationStore;
    private final OnDemandHierarchyExpander onDemandHierarchyExpander;
    private final GranularYouViewService youviewService;
    
    public OnDemandBasedRevocationProcessor(RevokedContentStore revocationStore,
            OnDemandHierarchyExpander onDemandHierarchyExpander, GranularYouViewService youviewService) {
        this.revocationStore = checkNotNull(revocationStore);
        this.onDemandHierarchyExpander = checkNotNull(onDemandHierarchyExpander);
        this.youviewService = checkNotNull(youviewService);
    }

    @Override
    public void revoke(Content content) {
        checkArgument(content instanceof Item, "content " + content.getCanonicalUri() + " not an item, cannot revoke");
        
        Item item = (Item) content;
        Set<String> onDemandIds = onDemandHierarchyExpander.expandHierarchy(item).keySet();

        for (String onDemandId : onDemandIds) {
            youviewService.sendDeleteFor(content, TVAElementType.ONDEMAND, onDemandId);
        }
        revocationStore.revoke(content.getCanonicalUri());
    }

    @Override
    public void unrevoke(Content content) {
        checkArgument(content instanceof Item, "content " + content.getCanonicalUri() + " not an item, cannot unrevoke");
        
        revocationStore.unrevoke(content.getCanonicalUri());
        
        Item item = (Item) content;
        Map<String, ItemOnDemandHierarchy> onDemandHierarchies = onDemandHierarchyExpander.expandHierarchy(item);
        
        for (Entry<String, ItemOnDemandHierarchy> onDemandHierarchy : onDemandHierarchies.entrySet()) {
            youviewService.uploadOnDemand(onDemandHierarchy.getValue(), onDemandHierarchy.getKey());
        }
    }
}
