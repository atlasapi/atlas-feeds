package org.atlasapi.feeds.youview.lovefilm;

import org.atlasapi.feeds.tvanytime.ChannelElementGenerator;
import org.atlasapi.media.channel.Channel;

import tva.metadata._2010.ServiceInformationType;

public class LoveFilmChannelGenerator implements ChannelElementGenerator {

    @Override
    public ServiceInformationType generate(Channel channel) {
        throw new UnsupportedOperationException("Channels are not supported for the LOVEFiLM publisher");
    }
}
