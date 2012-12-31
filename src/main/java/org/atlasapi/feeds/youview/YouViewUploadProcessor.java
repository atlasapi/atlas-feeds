package org.atlasapi.feeds.youview;

import org.atlasapi.media.content.Content;

public interface YouViewUploadProcessor<T> {
    
    boolean process(Iterable<Content> chunk);
    
    T getResult();
    
}
