package org.atlasapi.feeds.youview.tasks.processing;

import java.util.Map;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;


public class PublisherDelegatingTaskProcessor implements TaskProcessor {

    private final Map<Publisher, TaskProcessor> processors;
    
    public PublisherDelegatingTaskProcessor(Map<Publisher, TaskProcessor> processors) {
        this.processors = ImmutableMap.copyOf(processors);
    }
    
    @Override
    public void process(Task task) {
        TaskProcessor delegate = fetchDelegateOrThrow(task.publisher());
        delegate.process(task);
    }

    @Override
    public void checkRemoteStatusOf(Task task) {
        TaskProcessor delegate = fetchDelegateOrThrow(task.publisher());
        delegate.checkRemoteStatusOf(task);
    }
    
    private TaskProcessor fetchDelegateOrThrow(Publisher publisher) {
        TaskProcessor delegate = processors.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate;
    }
}
