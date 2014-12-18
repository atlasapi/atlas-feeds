package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.UnexpectedContentTypeException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
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
    private final GranularYouViewService service;
    private final VersionHierarchyExpander versionExpander;
    private final ContentHierarchyExtractor hierarchyExtractor;

    public ReferentialIntegrityCheckingReportHandler(GranularYouViewService service, ContentResolver contentResolver,
            VersionHierarchyExpander versionExpander, ContentHierarchyExtractor hierarchyExtractor) {
        this.service = checkNotNull(service);
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
        log.trace(String.format("Handling missing content for %s: %s, content %s", task.elementType().name(), task.elementId(), task.content()));
        switch (task.elementType()) {
        case BRAND:
            throw new RuntimeException(String.format("encountered missing content for brand %s: %s", task.content(), message.getComment().getValue()));
        case SERIES:
            handleMissingBrand(message, task);
            break;
        case ITEM:
            handleMissingContainer(message, task);
            break;
        case BROADCAST:
            handleMissingVersion(message, task);
            break;
        case ONDEMAND:
            handleMissingVersion(message, task);
            break;
        case VERSION:
            handleMissingItem(message, task);
            break;
        default:
            throw new RuntimeException("encountered unknown element type " + task.elementType().name());
        }
    }

    private void handleMissingBrand(ControlledMessageType message, Task task) {
        Content resolved = resolveContentFor(task.content());
        if (!(resolved instanceof Series)) {
            throw new UnexpectedContentTypeException(Series.class, resolved);
        }
        Series series = (Series) resolved;
        Optional<Brand> brand = hierarchyExtractor.brandFor(series);
        if (!brand.isPresent()) {
            throw new RuntimeException("unable to resolve expected brand for series " + task.content());
        }
        
        service.uploadContent(brand.get());
    }

    private void handleMissingContainer(ControlledMessageType message, Task task) {
        Content resolved = resolveContentFor(task.content());
        if (!(resolved instanceof Item)) {
            throw new UnexpectedContentTypeException(Item.class, resolved);
        }
        Optional<Series> series = hierarchyExtractor.seriesFor((Item) resolved);
        if (series.isPresent()) {
            service.uploadContent(series.get());
            return;
        }
        Optional<Brand> brand = hierarchyExtractor.brandFor((Item) resolved);
        if (brand.isPresent()) {        
            service.uploadContent(brand.get());
            return;
        }
        throw new RuntimeException("No series or brand found for item " + task.content() + ", unable to resolve semantic integrity error");
    }

    private void handleMissingItem(ControlledMessageType message, Task task) {
        Content resolved = resolveContentFor(task.content());
        service.uploadContent(resolved);        
    }

    private void handleMissingVersion(ControlledMessageType message, Task task) {
        Content resolved = resolveContentFor(task.content());
        if (!(resolved instanceof Item)) {
            throw new UnexpectedContentTypeException(Item.class, resolved);
        }
        String versionCrid = resolveVersionId(message.getComment());
        ItemAndVersion versionHierarchy = versionExpander.expandHierarchy((Item) resolved).get(versionCrid);
        if (versionHierarchy == null) {
            throw new RuntimeException("Missing version crid " + versionCrid + " is not a valid version crid for content " + task.content());
        }
        service.uploadVersion(versionHierarchy, versionCrid);
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
