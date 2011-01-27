package org.atlasapi.feeds.radioplayer.outputting;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

public class RadioPlayerBroadcastItem {

    private final Item item;
    private final Version version;
    private final Broadcast broadcast;

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
    
}
