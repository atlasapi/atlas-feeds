package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Content;


public interface YouViewDeleteProcessor<T> {
    
    boolean process(Content chunk);
    
    T getResult();
    
}
