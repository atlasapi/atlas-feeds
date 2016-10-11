package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.channel.Channel;

import tva.metadata._2010.ServiceInformationType;

public interface MasterbrandElementGenerator {

    ServiceInformationType generate(Channel channel);

}
