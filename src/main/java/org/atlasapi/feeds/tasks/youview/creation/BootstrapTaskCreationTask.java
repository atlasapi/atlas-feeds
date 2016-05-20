package org.atlasapi.feeds.tasks.youview.creation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.channel.ChannelType;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;


public class BootstrapTaskCreationTask extends TaskCreationTask {
    
    private final YouViewContentResolver contentResolver;
    private final DateTime startOfTime;

    public BootstrapTaskCreationTask(YouViewLastUpdatedStore lastUpdatedStore, Publisher publisher,
            ContentHierarchyExpander hierarchyExpander, IdGenerator idGenerator,
            TaskStore taskStore, TaskCreator taskCreator, PayloadCreator payloadCreator, 
            YouViewContentResolver contentResolver, YouViewPayloadHashStore payloadHashStore,
            DateTime startOfTime) {
        super(lastUpdatedStore, publisher, hierarchyExpander, idGenerator, taskStore, taskCreator,
                payloadCreator, payloadHashStore, HashCheck.IGNORE);
        this.contentResolver = checkNotNull(contentResolver);
        this.startOfTime = checkNotNull(startOfTime);
    }

    @Override
    protected void runTask() {
        DateTime lastUpdated = new DateTime();
        Iterator<Content> allContent = contentResolver.updatedSince(startOfTime);

        YouViewContentProcessor processor = contentProcessor(startOfTime, Action.UPDATE);
        YouViewChannelProcessor channelProcessor = channelProcessor(Action.UPDATE,
                ChannelType.CHANNEL);
        while (allContent.hasNext()) {
            if (!shouldContinue()) {
                return;
            }
            Content next = allContent.next();
            if (isActivelyPublished(next)) {
                processor.process(next);
                reportStatus(processor.getResult().toString());
            }
        }

        setLastUpdatedTime(lastUpdated);
    }

}
