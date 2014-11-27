package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Content;

public interface YouViewContentProcessor<T> {
    
    boolean process(Content content);
    
    T getResult();
    
}
