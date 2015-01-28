package org.atlasapi.feeds.tasks.youview.creation;

import org.atlasapi.media.entity.Content;

import com.metabroadcast.common.scheduling.UpdateProgress;


public interface YouViewContentProcessor {

    boolean process(Content content);
    
    UpdateProgress getResult();
}
