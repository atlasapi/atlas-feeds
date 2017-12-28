package org.atlasapi.feeds.tasks.youview.creation;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;

import com.metabroadcast.common.stream.MoreCollectors;
import com.metabroadcast.representative.api.IdChange;
import com.metabroadcast.representative.api.RepresentativeId;
import com.metabroadcast.representative.api.RepresentativeIdResponse;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.metabroadcast.representative.util.Utils.decode;

/**
 * This class queries the representative id service for any changes in equiv sets between now and
 * the last time it checked. It then creates new revoke and upload tasks accordingly.
 */
public class RepresentativeIdChangesHandlingTask extends DeltaTaskCreationTask {

    private static final Logger log = LoggerFactory.getLogger(RepresentativeIdChangesHandlingTask.class);

    public RepresentativeIdChangesHandlingTask(
            YouViewLastUpdatedStore lastUpdatedStore, Publisher publisher,
            ContentHierarchyExpander hierarchyExpander,
            IdGenerator idGenerator, TaskStore taskStore,
            TaskCreator taskCreator, PayloadCreator payloadCreator,
            UpdateTask updateTask,
            YouViewContentResolver contentResolver,
            YouViewPayloadHashStore payloadHashStore,
            ChannelResolver channelResolver,
            KnownTypeQueryExecutor mergingResolver) {
        super(
                lastUpdatedStore,
                publisher,
                hierarchyExpander,
                idGenerator,
                taskStore,
                taskCreator,
                payloadCreator,
                updateTask,
                contentResolver,
                payloadHashStore,
                channelResolver,
                mergingResolver
        );
    }

    @Override
    protected void runTask() {
        Optional<DateTime> lastCheckedOptional = getLastRepIdChangesChecked();
        if (!lastCheckedOptional.isPresent()) {
            throw new IllegalStateException("The representative id changes have never been checked "
                                            + "before for "+ getPublisher()
                                            + ". It would be insensible to check the status since"
                                            + "forever. Open a mongo console and add a date. "
                                            + "Example call, from atlas-split db, "
                                            + "db.youviewLastUpdated.update({_id:\""+ getPublisher()+"\"},{$set:{lastRepIdChangesChecked:new ISODate(\"2017-12-18T14:58:00.002Z\")}}))");
        }

        // despite the intermediate conversion, db dates are UTC.
        Instant lastChecked = Instant.parse(lastCheckedOptional.toString());
        log.info("Started a repId status check for {} from {}", getPublisher(), lastChecked);
        DateTime startOfTask = new DateTime();

        //get processors that will handle things irrespective of creation date.
        YouViewContentProcessor uploadProcessor =
                contentProcessor(DateTime.parse("2010-12-22T23:51:13.120Z"), Action.UPDATE);
        YouViewContentProcessor deletionProcessor =
                contentProcessor(DateTime.parse("2010-12-22T23:51:13.120Z"), Action.DELETE);

        List<Content> contentToDeleted = ImmutableList.of();
        List<Content> contentToUploaded = ImmutableList.of();

        //handle the response.
        RepresentativeIdResponse repResponse = getRepIdClient().getChangesSince(lastChecked);
        for (IdChange idChange : repResponse.getChanges()) {

            ImmutableMap<String, RepresentativeId> from = toMap(idChange.getFrom());
            ImmutableMap<String, RepresentativeId> to = toMap(idChange.getTo());

            //Everything in that set is now represented by something else.
            for (String id : from.keySet()) {
                //we need to remove the parent, but also all the the dependents such as versions.
                //e.g. for set A(A, B) we need to remove Item A, and dependents for A & B
                //Because we swap the id of all items in the set to match the repId, only A parents
                //will be removed, and all dependents removed will be linked to A.
                ImmutableSet<Content> toBeDeleted = from.get(id).getSameAs().stream()
                        .map(RepresentativeIdChangesHandlingTask::resolve)
                        .peek(c -> c.setId(decode(id))) //because they where created with this id
                        .collect(MoreCollectors.toImmutableSet());
                contentToDeleted.addAll(toBeDeleted);
            }

            //If wanted to do more precise movements, we should find the FROM repIds no longer
            //used and remove those and their dependents, then find what remains in FROM and remove
            //only the dependents. However, that would require us to write the code that can handle
            //dependents only. Instead, we'll use the existing deletionProcessor to revoke
            // everything, and then recreate it under its new status.

            // Now we can just reupload the whole TO set using the normal pipeline. That will
            // pick up the latest changes and handle both new items and larger equiv sets.
            // if equiv set has changed again in the meantime that's ok. This routine will
            // pick up the changes and also revoke them.
            ImmutableSet<Content> toBeUploaded = to.keySet().stream()
                    .map(RepresentativeIdChangesHandlingTask::resolve)
                    .collect(MoreCollectors.toImmutableSet());
            contentToUploaded.addAll(toBeUploaded);
        }

        // All that is left now is to process things. We'll handle the revokes first
        // and the updates second. The order matters, since otherwise we might end up with versions
        // being completely revoked rather than revoked from one item and reuploaded under another.
        List<Content> orderedForDeletion = sortContentForDeletion(contentToDeleted);
        for (Content toBeDeleted : orderedForDeletion) {
            deletionProcessor.process(toBeDeleted);
            reportStatus("Deletes: " + deletionProcessor.getResult());
        }

        if(getPublisher().equals(Publisher.AMAZON_UNBOX)){
            //this method will run equiv, merge and repId the item again.
            super.uploadFromAmazon(contentToUploaded.iterator(), uploadProcessor);
        } else {
            throw new IllegalStateException("Handling repId changes is not supported for "+getPublisher());
        }

        log.info("Done creating {} tasks to handle RepId changes until {}.", getPublisher(), startOfTask);
        setLastRepIdChecked(startOfTask);
        reportStatus("Done handling repId changes");
    }

    //Now we need to convert the IDs to Content.
    //TODO: fuck-my-life. We are looking for a mechanism to resolve content by id. We kinda want
    //to remove THIS content and not the EQUIVALATED content, because if the equiv graph has changed
    //we might not remove what we where trying to remove.
    private static Content resolve (String id){
        return new Content() {

            @Override
            public Described copy() {
                return new Item();
            }
        };
    }

    //Map from repId to the repId-object.
    private static ImmutableMap<String, RepresentativeId> toMap(Set<RepresentativeId> set) {
        return set.stream()
                .collect(MoreCollectors.toImmutableMap(RepresentativeId::getId, i -> i));
    }

    private static ImmutableSet<String> getRepIds(Set<RepresentativeId> set) {
        return set.stream()
                .map(RepresentativeId::getId)
                .collect(MoreCollectors.toImmutableSet());
    }

}
