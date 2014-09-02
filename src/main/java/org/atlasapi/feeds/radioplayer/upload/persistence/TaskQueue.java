package org.atlasapi.feeds.radioplayer.upload.persistence;

import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.QueueTask;

import com.google.common.base.Optional;


public interface TaskQueue<T extends QueueTask> {

    // TODO javadoc
    void push(T task);
    Optional<T> fetchOne();
    boolean remove(T task);
    boolean contains(RadioPlayerFile file);
}
