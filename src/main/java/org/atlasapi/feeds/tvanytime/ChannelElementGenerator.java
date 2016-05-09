package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.channel.Channel;

import tva.metadata._2010.ServiceInformationType;

public interface ChannelElementGenerator {
    ServiceInformationType generate(Channel channel);
    ServiceInformationType generate(Channel channel, Channel parentChannel);
}
