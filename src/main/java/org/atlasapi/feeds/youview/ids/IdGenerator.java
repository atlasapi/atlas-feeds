package org.atlasapi.feeds.youview.ids;

import java.util.List;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;


public interface IdGenerator {

    String generateVersionCrid(Item item, Version version);
    String generateContentCrid(Content content);
    String generateOnDemandImi(Item item, Version version, Encoding encoding, List<Location> locations);
    String generateBroadcastImi(String youViewServiceId, Broadcast broadcast);
    String generateChannelCrid(Channel channel);
    
}
