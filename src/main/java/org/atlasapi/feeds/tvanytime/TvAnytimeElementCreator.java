package org.atlasapi.feeds.tvanytime;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Content;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata._2010.ServiceInformationType;

public interface TvAnytimeElementCreator {

    GroupInformationType createGroupInformationElementFor(Content content);
    ProgramInformationType createProgramInformationElementFor(ItemAndVersion version, String versionCrid);
    OnDemandProgramType createOnDemandElementFor(ItemOnDemandHierarchy onDemand, String onDemandImi);
    BroadcastEventType createBroadcastEventElementFor(ItemBroadcastHierarchy broadcast, String broadcastImi);
    ServiceInformationType createChannelElementFor(Channel channel, Channel parentChannel);
    ServiceInformationType createMasterbrandElementFor(Channel channel);
}
