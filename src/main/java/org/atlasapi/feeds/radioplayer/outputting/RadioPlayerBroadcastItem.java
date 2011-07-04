package org.atlasapi.feeds.radioplayer.outputting;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

public class RadioPlayerBroadcastItem implements Comparable<RadioPlayerBroadcastItem> {

    private final Item item;
    private final Version version;
    private final Broadcast broadcast;
    private Container<?> container;

    public RadioPlayerBroadcastItem(Item item, Version version, Broadcast broadcast) {
        this.item = item;
        this.version = version;
        this.broadcast = broadcast;
    }

    public Item getItem() {
        return item;
    }

    public Version getVersion() {
        return version;
    }

    public Broadcast getBroadcast() {
        return broadcast;
    }

    @Override
    public int compareTo(RadioPlayerBroadcastItem that) {
        return this.broadcast.getTransmissionTime().compareTo(that.getBroadcast().getTransmissionTime());
    }

    public RadioPlayerBroadcastItem withContainer(Container<?> container) {
        this.container = container;
        return this;
    }
    
    public Container<?> getContainer() {
        return this.container;
    }

    public boolean hasContainer() {
        return container != null;
    }
    
}
