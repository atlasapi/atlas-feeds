package org.atlasapi.feeds.youview.hierarchy;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

/**
 * Wrapper for the Broadcast portion of an {@link Item}'s version hierarchy:
 * Item, {@link Version}, {@link Broadcast}, and the YouView service ID. This 
 * class holds one instance of each.
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public final class ItemBroadcastHierarchy {
    
    private final Item item;
    private final Version version;
    private final Broadcast broadcast;
    private final String youViewServiceId;
    
    public ItemBroadcastHierarchy(Item item, Version version, 
            Broadcast broadcast, String youViewServiceId) {
        this.item = checkNotNull(item);
        this.version = checkNotNull(version);
        this.broadcast = checkNotNull(broadcast);
        this.youViewServiceId = checkNotNull(youViewServiceId);
    }
    
    public Item item() {
        return item;
    }
    
    public Version version() {
        return version;
    }
    
    public Broadcast broadcast() {
        return broadcast;
    }
    
    public String youViewServiceId() {
        return youViewServiceId;
    }
}
