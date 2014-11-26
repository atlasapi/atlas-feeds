package org.atlasapi.feeds.youview.hierarchy;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;

/**
 * Wrapper for the On-Demand portion of an {@link Item}'s version hierarchy:
 * Item, {@link Version}, {@link Encoding}, {@link Location}. This class holds one instance
 * of each.
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public final class ItemOnDemandHierarchy {
    
    private final Item item;
    private final Version version;
    private final Encoding encoding;
    private final Location location;
    
    public ItemOnDemandHierarchy(Item item, Version version, 
            Encoding encoding, Location location) {
        this.item = checkNotNull(item);
        this.version = checkNotNull(version);
        this.encoding = checkNotNull(encoding);
        this.location = checkNotNull(location);
    }
    
    public Item item() {
        return item;
    }
    
    public Version version() {
        return version;
    }
    
    public Encoding encoding() {
        return encoding;
    }
    
    public Location location() {
        return location;
    }
}
