package org.atlasapi.feeds.youview.client;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.creation.TaskCreator;
import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.IdGeneratorFactory;
import org.atlasapi.feeds.youview.UnexpectedContentTypeException;
import org.atlasapi.feeds.youview.YouviewContentMerger;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.feeds.youview.persistence.HashType;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.unbox.AmazonContentConsolidator;
import org.atlasapi.feeds.youview.unbox.AmazonIdGenerator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tva.mpeg7._2008.TextualType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;
import static org.atlasapi.feeds.tasks.youview.creation.TaskCreationTask.save;

public class ReferentialIntegrityCheckingReportHandler implements YouViewReportHandler {

    private static final Map<Publisher, Pattern> PUBLISHER_TO_PATTERN_MAP = ImmutableMap.of(
            Publisher.BBC_NITRO, Pattern.compile("crid://nitro.bbc.co.uk/iplayer/youview/[a-z0-9]*"),
            Publisher.AMAZON_UNBOX, AmazonIdGenerator.getVersionCridPattern()
    );

    private static final String REFERENTIAL_INTEGRITY_REASON
            = "http://refdata.youview.com/mpeg7cs/YouViewMetadataIngestReasonCS/2010-09-23#semantic-referential_integrity";
    
    
    private final Logger log = LoggerFactory.getLogger(ReferentialIntegrityCheckingReportHandler.class);
    
    private final ContentResolver contentResolver;
    private final TaskCreator taskCreator;
    private final TaskStore taskStore;
    private final YouViewPayloadHashStore payloadHashStore;
    private final ContentHierarchyExtractor hierarchyExtractor;
    private PayloadCreator payloadCreator;
    private final YouviewContentMerger amazonContentMerger;

    public ReferentialIntegrityCheckingReportHandler(
            TaskCreator taskCreator,
            TaskStore taskStore,
            YouViewPayloadHashStore payloadHashStore,
            PayloadCreator payloadCreator,
            ContentResolver contentResolver,
            ContentHierarchyExtractor hierarchyExtractor,
            YouviewContentMerger amazonContentMerger) {

        this.taskCreator = checkNotNull(taskCreator);
        this.taskStore = checkNotNull(taskStore);
        this.payloadHashStore = checkNotNull(payloadHashStore);
        this.payloadCreator = checkNotNull(payloadCreator);
        this.contentResolver = checkNotNull(contentResolver);
        this.hierarchyExtractor = checkNotNull(hierarchyExtractor);
        this.amazonContentMerger = checkNotNull(amazonContentMerger);
    }

    @Override
    public void handle(TransactionReportType report, Task task) {
        if (TransactionStateType.QUARANTINED.equals(report.getState())) {
            for (FragmentReportType updateReport : report.getFragmentUpdateReport()) {
                if (!updateReport.isSuccess()) {
                    for (ControlledMessageType message : updateReport.getMessage()) {
                        if (REFERENTIAL_INTEGRITY_REASON.equals(message.getReasonCode())) {
                            handleMissingContent(message, task);
                        }
                    }
                }
            }
        }
    }

    private void handleMissingContent(ControlledMessageType message, Task task) {
        Preconditions.checkArgument(YOUVIEW.equals(task.destination().type()), "Expected feed type " + YOUVIEW.name() + ", was " + task.destination().type() + " for task " + task.id());

        YouViewDestination destination = (YouViewDestination) task.destination();
        log.info(String.format(
                "Handling missing content for %s: %s, content %s", 
                destination.elementType().name(), 
                destination.elementId(), 
                destination.contentUri()
        ));
        try {
            switch (destination.elementType()) {
            case BRAND:
                throw new RuntimeException(String.format(
                        "encountered missing content for brand %s: %s", 
                        destination.contentUri(), 
                        message.getComment().getValue()
                        ));
            case SERIES:
                handleMissingBrand(message, destination.contentUri());
                break;
            case ITEM:
                handleMissingContainer(message, destination.contentUri());
                break;
            case BROADCAST:
                handleMissingVersion(message, destination.contentUri());
                break;
            case ONDEMAND:
                handleMissingVersion(message, destination.contentUri());
                break;
            case VERSION:
                handleMissingItem(message, destination.contentUri());
                break;
            default:
                throw new RuntimeException("encountered unknown element type " + destination.elementType().name());
            }
        } catch(Exception e) {
            log.error("failed to handle missing element for task {}", task.id(), e);
        }
    }

    private void handleMissingBrand(ControlledMessageType message, String contentUri) throws PayloadGenerationException {
        Content resolved = resolveContentFor(contentUri);
        if (!(resolved instanceof Series)) {
            throw new UnexpectedContentTypeException(Series.class, resolved);
        }
        Series series = (Series) resolved;

        Optional<Brand> brandOptional = hierarchyExtractor.brandFor(series);
        if (!brandOptional.isPresent()) {
            throw new RuntimeException("unable to resolve expected brand for series " + contentUri);
        }

        createAndWriteTaskFor(amazonProcessing(brandOptional.get()));
    }

    //Do amazon specific processing. Dem hacks.
    private Content amazonProcessing(Content parentContent) {
        if (parentContent.getPublisher().equals(Publisher.AMAZON_UNBOX)) {
            try {
                parentContent = amazonContentMerger.equivAndMerge(parentContent);
            } catch (Exception e) {
                log.error("Failed during the attempt to equiv, merge or get a repId. "
                          + "The attempted Content was {}.",
                        parentContent.getCanonicalUri(), e
                );
            }
            try {
                AmazonContentConsolidator.consolidate(parentContent); //mutates the item
            } catch (Exception e) {
                log.error("Failed during the attempt to consolidate versions. "
                          + " The attempted Content was {}.",
                       parentContent.getCanonicalUri(), e
                );
            }
        }

        return parentContent;
    }

    private void handleMissingContainer(ControlledMessageType message, String contentUri) throws PayloadGenerationException {
        Content resolved = resolveContentFor(contentUri);
        if (!(resolved instanceof Item)) {
            throw new UnexpectedContentTypeException(Item.class, resolved);
        }
        Optional<Series> seriesOpt = hierarchyExtractor.seriesFor((Item) resolved);
        if (seriesOpt.isPresent()) {
            createAndWriteTaskFor(amazonProcessing(seriesOpt.get()));
            return;
        }
        Optional<Brand> brandOpt = hierarchyExtractor.brandFor((Item) resolved);
        if (brandOpt.isPresent()) {
            createAndWriteTaskFor(amazonProcessing(brandOpt.get()));
            return;
        }
        throw new RuntimeException("No series or brand found for item " + contentUri + ", unable to resolve semantic integrity error");
    }

    private void handleMissingItem(ControlledMessageType message, String contentUri) throws PayloadGenerationException {
        Content resolved = resolveContentFor(contentUri);
        createAndWriteTaskFor(amazonProcessing(resolved));
    }

    private void createAndWriteTaskFor(Content content) throws PayloadGenerationException {
        IdGenerator idGenerator = IdGeneratorFactory.create(content.getPublisher());
        String contentCrid = idGenerator.generateContentCrid(content);
        Payload p = payloadCreator.payloadFrom(contentCrid, content);
        if (shouldSave(HashType.CONTENT, contentCrid, p)) {
            if (Publisher.AMAZON_UNBOX.equals(content.getPublisher())) {
                save(payloadHashStore, taskStore, taskCreator.taskFor(contentCrid, content, p, Action.UPDATE));
            } else {
                taskStore.save(taskCreator.taskFor(contentCrid, content, p, Action.UPDATE));
                payloadHashStore.saveHash(HashType.CONTENT, contentCrid, p.hash());
            }
        } else{
            log.debug("Existing hash found for Content {}, not updating", contentCrid);
        }
    }

    private void handleMissingVersion(ControlledMessageType message, String contentUri) throws PayloadGenerationException {
        Content resolved = resolveContentFor(contentUri);
        if (!(resolved instanceof Item)) {
            throw new UnexpectedContentTypeException(Item.class, resolved);
        }
        resolved = amazonProcessing(resolved);
        String versionCrid = resolveVersionId(resolved.getPublisher(), message.getComment());
        IdGenerator idGenerator = IdGeneratorFactory.create(resolved.getPublisher());
        VersionHierarchyExpander versionExpander = new VersionHierarchyExpander(idGenerator);
        ItemAndVersion versionHierarchy = versionExpander.expandHierarchy((Item) resolved).get(versionCrid);
        if (versionHierarchy == null) {
            throw new RuntimeException("Missing version crid " + versionCrid + " is not a valid version crid for content " + contentUri);
        }
        Payload p = payloadCreator.payloadFrom(versionCrid, versionHierarchy);
        if (shouldSave(HashType.CONTENT, versionCrid, p)) {
            if (Publisher.AMAZON_UNBOX.equals(resolved.getPublisher())) {
                save(payloadHashStore, taskStore, taskCreator.taskFor(versionCrid, resolved, p, Action.UPDATE));
            } else {
                taskStore.save(taskCreator.taskFor(versionCrid, versionHierarchy, p, Action.UPDATE));
                payloadHashStore.saveHash(HashType.CONTENT, versionCrid, p.hash());
            }
        } else {
            log.debug("Existing hash found for Content {}, not updating", versionCrid);
        }
    }

    private String resolveVersionId(Publisher publisher, TextualType comment) {
        Pattern pattern = PUBLISHER_TO_PATTERN_MAP.get(publisher);
        Matcher matcher = pattern.matcher(comment.getValue());
        if (!matcher.find()) {
            throw new RuntimeException("Unable to match version crid pattern in comment: " + comment.getValue());
        }
        return matcher.group();
    }

    private Content resolveContentFor(String contentUri) {
        ResolvedContent resolved = contentResolver.findByCanonicalUris(ImmutableList.of(contentUri));
        Content content = (Content) resolved.asResolvedMap().get(contentUri);
        if (content == null) {
            throw new RuntimeException("Unable to resolve content for uri " + contentUri);
        }
        return content;
    }

    private boolean shouldSave(HashType type, String id, Payload payload) {
        java.util.Optional<String> storedHash = payloadHashStore.getHash(type, id);
        //if the hash changed, or is not there, we should save.
        return storedHash.map(payload::hasChanged).orElse(true);
    }

}
