package org.atlasapi.feeds.youview.tasks.creation;

import org.atlasapi.media.entity.Content;

import com.metabroadcast.common.scheduling.UpdateProgress;


public interface YouViewContentProcessor {

    boolean process(Content content);
    
    UpdateProgress getResult();
}
