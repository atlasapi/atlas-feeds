package org.atlasapi.feeds.youview.hierarchy;

import java.util.List;

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
    private final List<Location> locations; //multiple locations represent the same item
    // being available in multiple locations with the same uri, for example being available
    // to buy, to rent, and under subscription.
    
    public ItemOnDemandHierarchy(
            Item item,
            Version version,
            Encoding encoding,
            List<Location> locations) {

        this.item = checkNotNull(item);
        this.version = checkNotNull(version);
        this.encoding = checkNotNull(encoding);
        this.locations = checkNotNull(locations);
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
    
    public List<Location> locations() {
        return locations;
    }
}
