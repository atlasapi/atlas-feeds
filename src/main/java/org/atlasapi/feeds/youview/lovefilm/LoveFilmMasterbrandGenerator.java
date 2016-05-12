package org.atlasapi.feeds.youview.lovefilm;

import org.atlasapi.feeds.tvanytime.MasterbrandElementGenerator;
import org.atlasapi.media.channel.Channel;

import tva.metadata._2010.ServiceInformationType;

public class LoveFilmMasterbrandGenerator implements MasterbrandElementGenerator {

    @Override
    public ServiceInformationType generate(Channel channel) {
        throw new UnsupportedOperationException("Channels are not supported for the LOVEFiLM publisher");
    }
}
