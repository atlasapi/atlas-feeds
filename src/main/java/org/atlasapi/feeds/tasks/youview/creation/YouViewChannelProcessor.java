package org.atlasapi.feeds.tasks.youview.creation;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Content;

import com.metabroadcast.common.scheduling.UpdateProgress;

public interface YouViewChannelProcessor {

    boolean process(Channel content);

    UpdateProgress getResult();

}
