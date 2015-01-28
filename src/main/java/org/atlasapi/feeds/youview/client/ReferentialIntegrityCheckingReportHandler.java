package org.atlasapi.feeds.youview.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.creation.TaskCreator;
import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.UnexpectedContentTypeException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.mpeg7._2008.TextualType;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class ReferentialIntegrityCheckingReportHandler implements YouViewReportHandler {

    private static final Pattern VERSION_CRID_PATTERN = Pattern.compile("crid://nitro.bbc.co.uk/iplayer/youview/[a-z0-9]*");
    private static final String REFERENTIAL_INTEGRITY_REASON 
            = "http://refdata.youview.com/mpeg7cs/YouViewMetadataIngestReasonCS/2010-09-23#semantic-referential_integrity";
    
    
    private final Logger log = LoggerFactory.getLogger(ReferentialIntegrityCheckingReportHandler.class);
    
    private final ContentResolver contentResolver;
    private final TaskCreator taskCreator;
    private final IdGenerator idGenerator;
    private final TaskStore taskStore;
    private final VersionHierarchyExpander versionExpander;
    private final ContentHierarchyExtractor hierarchyExtractor;

    public ReferentialIntegrityCheckingReportHandler(TaskCreator taskCreator, IdGenerator idGenerator,
            TaskStore taskStore, ContentResolver contentResolver, VersionHierarchyExpander versionExpander,
            ContentHierarchyExtractor hierarchyExtractor) {
        this.taskCreator = checkNotNull(taskCreator);
        this.taskStore = checkNotNull(taskStore);
        this.idGenerator = checkNotNull(idGenerator);
        this.contentResolver = checkNotNull(contentResolver);
        this.versionExpander = checkNotNull(versionExpander);
        this.hierarchyExtractor = checkNotNull(hierarchyExtractor);
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
        log.trace(String.format(
                "Handling missing content for %s: %s, content %s", 
                destination.elementType().name(), 
                destination.elementId(), 
                destination.contentUri()
        ));
        
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
    }

    private void handleMissingBrand(ControlledMessageType message, String contentUri) {
        Content resolved = resolveContentFor(contentUri);
        if (!(resolved instanceof Series)) {
            throw new UnexpectedContentTypeException(Series.class, resolved);
        }
        Series series = (Series) resolved;
        Optional<Brand> brand = hierarchyExtractor.brandFor(series);
        if (!brand.isPresent()) {
            throw new RuntimeException("unable to resolve expected brand for series " + contentUri);
        }
        
        createAndWriteTaskFor(brand.get());
    }

    private void handleMissingContainer(ControlledMessageType message, String contentUri) {
        Content resolved = resolveContentFor(contentUri);
        if (!(resolved instanceof Item)) {
            throw new UnexpectedContentTypeException(Item.class, resolved);
        }
        Optional<Series> series = hierarchyExtractor.seriesFor((Item) resolved);
        if (series.isPresent()) {
            createAndWriteTaskFor(series.get());
            return;
        }
        Optional<Brand> brand = hierarchyExtractor.brandFor((Item) resolved);
        if (brand.isPresent()) {       
            createAndWriteTaskFor(brand.get());
            return;
        }
        throw new RuntimeException("No series or brand found for item " + contentUri + ", unable to resolve semantic integrity error");
    }

    private void handleMissingItem(ControlledMessageType message, String contentUri) {
        Content resolved = resolveContentFor(contentUri);
        createAndWriteTaskFor(resolved);
    }

    private void createAndWriteTaskFor(Content content) {
        String contentCrid = idGenerator.generateContentCrid(content);
        Task task = taskCreator.taskFor(contentCrid, content, Action.UPDATE);
        taskStore.save(task);
    }

    private void handleMissingVersion(ControlledMessageType message, String contentUri) {
        Content resolved = resolveContentFor(contentUri);
        if (!(resolved instanceof Item)) {
            throw new UnexpectedContentTypeException(Item.class, resolved);
        }
        String versionCrid = resolveVersionId(message.getComment());
        ItemAndVersion versionHierarchy = versionExpander.expandHierarchy((Item) resolved).get(versionCrid);
        if (versionHierarchy == null) {
            throw new RuntimeException("Missing version crid " + versionCrid + " is not a valid version crid for content " + contentUri);
        }
        Task task = taskCreator.taskFor(versionCrid, versionHierarchy, Action.UPDATE);
        taskStore.save(task);
    }

    private String resolveVersionId(TextualType comment) {
        Matcher matcher = VERSION_CRID_PATTERN.matcher(comment.getValue());
        if (!matcher.find()) {
            throw new RuntimeException("unable to match version crid pattern in comment: " + comment.getValue());
        }
        return matcher.group();
    }

    private Content resolveContentFor(String contentUri) {
        ResolvedContent resolved = contentResolver.findByCanonicalUris(ImmutableList.of(contentUri));
        Content content = (Content) resolved.asResolvedMap().get(contentUri);
        if (content == null) {
            throw new RuntimeException("unable to resolve content for uri " + contentUri);
        }
        return content;
    }

}
