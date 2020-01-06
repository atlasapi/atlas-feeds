package org.atlasapi.feeds.radioplayer.outputting;

import com.google.common.base.Function;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import javax.annotation.Nullable;
import java.util.Comparator;

public class RadioPlayerBroadcastItem implements Comparable<RadioPlayerBroadcastItem> {

    private static final Comparator<Broadcast> broadcastComparator = Comparator.nullsLast(
            Comparator.comparing(Broadcast::getTransmissionTime)
    );

    private final Item item;
    private final Version version;
    @Nullable private final Broadcast broadcast;
    private Container container;

    public RadioPlayerBroadcastItem(Item item, Version version, @Nullable Broadcast broadcast) {
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

    @Nullable
    public Broadcast getBroadcast() {
        return broadcast;
    }

    @Override
    public int compareTo(RadioPlayerBroadcastItem that) {
        return broadcastComparator.compare(this.getBroadcast(), that.getBroadcast());
    }

    public RadioPlayerBroadcastItem withContainer(Container container) {
        this.container = container;
        return this;
    }
    
    public Container getContainer() {
        return this.container;
    }

    public boolean hasContainer() {
        return container != null;
    }
    
    public static final Function<RadioPlayerBroadcastItem, Item> TO_ITEM = new Function<RadioPlayerBroadcastItem, Item>() {
        @Override
        public Item apply(RadioPlayerBroadcastItem input) {
            return input.getItem();
        }
    };
}
