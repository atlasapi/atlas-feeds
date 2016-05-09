package org.atlasapi.feeds.youview.unbox;

import org.atlasapi.feeds.tvanytime.ChannelElementGenerator;
import org.atlasapi.media.channel.Channel;

import tva.metadata._2010.ServiceInformationType;

public class UnboxChannelGenerator implements ChannelElementGenerator {

    @Override
    public ServiceInformationType generate(Channel channel) {
        throw new UnsupportedOperationException("Cahnnels are not supported for the Unbox publisher");
    }
}
