package org.atlasapi.feeds.tasks.youview.creation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.scheduling.ScheduledTask;


public class DeltaTaskCreationTask extends TaskCreationTask {

    private static final Ordering<Content> HIERARCHICAL_ORDER = new HierarchicalOrdering();
    
    private final YouViewContentResolver contentResolver;
    private final UpdateTask updateTask;

    public DeltaTaskCreationTask(YouViewLastUpdatedStore lastUpdatedStore, Publisher publisher,
            ContentHierarchyExpander hierarchyExpander, IdGenerator idGenerator,
            TaskStore taskStore, TaskCreator taskCreator, PayloadCreator payloadCreator, UpdateTask updateTask, YouViewContentResolver contentResolver) {
        super(lastUpdatedStore, publisher, hierarchyExpander, idGenerator, taskStore, taskCreator,
                payloadCreator);
        this.contentResolver = checkNotNull(contentResolver);
        this.updateTask = checkNotNull(updateTask);
    }

    @Override
    protected void runTask() {
        Optional<DateTime> lastUpdated = getLastUpdatedTime();
        if (!lastUpdated.isPresent()) {
            throw new RuntimeException("The bootstrap has not successfully run. Please run the bootstrap upload and ensure that it succeeds before running the delta upload.");
        }
        
        Optional<DateTime> startOfTask = Optional.of(new DateTime());
        
        Iterator<Content> updatedContent = contentResolver.updatedSince(lastUpdated.get());
        
        List<Content> deleted = Lists.newArrayList();
        
        YouViewContentProcessor uploadProcessor = contentProcessor(lastUpdated.get(), Action.UPDATE);
        YouViewContentProcessor deletionProcessor = contentProcessor(lastUpdated.get(), Action.DELETE);
        
        while (updatedContent.hasNext()) {
            Content updated = updatedContent.next();
            if (updated.isActivelyPublished()) {
                uploadProcessor.process(updated);
                reportStatus("Uploads: " + uploadProcessor.getResult());
            } else {
                deleted.add(updated);
            }
        }
        
        List<Content> orderedForDeletion = orderContentForDeletion(deleted);

        for (Content toBeDeleted : orderedForDeletion) {
            deletionProcessor.process(toBeDeleted);
            reportStatus("Deletes: " + deletionProcessor.getResult());
        }
        
        setLastUpdatedTime(startOfTask.get());
        
        reportStatus("Uploading tasks to YouView");
        
        // temporary fix; too many txns are being generated, due to the separation of 
        // task generation and upload. Moving the upload to happen in sequence after
        // task generation should help.
        
        updateTask.run();
        
        reportStatus("Done uploading tasks to YouView");
    }
    
    private static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }
}
