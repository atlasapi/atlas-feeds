package org.atlasapi.feeds.youview.amazon;

import org.atlasapi.feeds.tvanytime.MasterbrandElementGenerator;
import org.atlasapi.media.channel.Channel;
import tva.metadata._2010.ServiceInformationType;

public class AmazonMasterbrandGenerator implements MasterbrandElementGenerator {

    @Override
    public ServiceInformationType generate(Channel channel) {
        throw new UnsupportedOperationException("Channels are not supported for the Unbox publisher");
    }
}
