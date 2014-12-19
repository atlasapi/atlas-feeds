package org.atlasapi.feeds.youview.hierarchy;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Objects;


/**
 * A wrapper for an {@link Item} and a {@link Version}
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public class ItemAndVersion {

    private final Item item;
    private final Version version;
    
    public ItemAndVersion(Item item, Version version) {
        this.item = checkNotNull(item);
        this.version = checkNotNull(version);
    }
    
    public Item item() {
        return item;
    }
    
    public Version version() {
        return version;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(item, version);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        
        if (!(that instanceof ItemAndVersion)) {
            return false;
        }
        
        ItemAndVersion other = (ItemAndVersion) that;
        return item.equals(other.item)
                && version.equals(other.version);
    }
}
