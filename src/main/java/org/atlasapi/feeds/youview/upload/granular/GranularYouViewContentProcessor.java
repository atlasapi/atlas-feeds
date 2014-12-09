package org.atlasapi.feeds.youview.upload.granular;

import org.atlasapi.media.entity.Content;

public interface GranularYouViewContentProcessor<T> {
    
    boolean process(Content content);
    
    T getResult();
    
}
