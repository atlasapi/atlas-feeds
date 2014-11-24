package org.atlasapi.feeds.youview.upload;

import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Content;


public interface YouViewClient {

    void upload(Content content);

    void sendDeleteFor(Content content);
    
    void checkRemoteStatusOf(Task transaction);
    
    void revoke(Content content);
    
    void unrevoke(Content content);
}
