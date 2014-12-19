package org.atlasapi.feeds.youview.upload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.junit.Test;

import tva.mpeg7._2008.TextualType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.base.Maybe;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class ReferentialIntegrityCheckingReportHandlerTest {

    private static final String REFERENTIAL_INTEGRITY_ERROR_REASON_CODE = 
            "http://refdata.youview.com/mpeg7cs/YouViewMetadataIngestReasonCS/2010-09-23#semantic-referential_integrity";
    private static final String REFERENTIAL_INTEGRITY_ERROR_PATTERN = 
            "Referenced %s (crid \"%s\") cannot be found";
    
    private GranularYouViewService service = mock(GranularYouViewService.class);
    private ContentResolver contentResolver = mock(ContentResolver.class);
    private IdGenerator idGenerator = mock(IdGenerator.class);
    private VersionHierarchyExpander versionExpander = new VersionHierarchyExpander(idGenerator);
    private ContentHierarchyExtractor hierarchyExtractor = mock(ContentHierarchyExtractor.class);
    
    private final YouViewReportHandler handler = new ReferentialIntegrityCheckingReportHandler(service, contentResolver, versionExpander, hierarchyExtractor);
    
    @Test(expected = RuntimeException.class)
    public void testThrowsIfReferentialIntegrityErrorGivenForBrand() {
        TransactionReportType report = createReportWithRefError("Series", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Brand brand = createBrand();
        
        handler.handle(report, createTaskFor(brand, TVAElementType.BRAND));
    }

    @Test
    public void testResolvesAndUploadsBrandIfReferentialIntegrityErrorGivenForSeriesWithBrand() {
        TransactionReportType report = createReportWithRefError("Brand", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Brand brand = createBrand();
        Series series = createSeries();
        
        when(hierarchyExtractor.brandFor(series)).thenReturn(Optional.of(brand));
        
        handler.handle(report, createTaskFor(series, TVAElementType.SERIES));
        
        verify(service).uploadContent(brand);
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfReferentialIntegrityErrorGivenForSeriesWithoutBrand() {
        TransactionReportType report = createReportWithRefError("Brand", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Series series = createSeries();
        
        when(hierarchyExtractor.brandFor(series)).thenReturn(Optional.<Brand>absent());
        
        handler.handle(report, createTaskFor(series, TVAElementType.SERIES));
    }
    
    @Test(expected = RuntimeException.class)
    public void testThrowsIfReferentialIntegrityErrorGivenForItemWithoutBrandOrSeries() {
        TransactionReportType report = createReportWithRefError("Series", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Item item = createItem();
        
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.<Series>absent());
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.<Brand>absent());
        
        handler.handle(report, createTaskFor(item, TVAElementType.ITEM));
    }

    @Test
    public void testResolvesAndUploadsSeriesIfReferentialIntegrityErrorGivenForItemWithSeriesButNoBrand() {
        TransactionReportType report = createReportWithRefError("Series", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Item item = createItem();
        Series series = createSeries();
        
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.of(series));
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.<Brand>absent());
        
        
        handler.handle(report, createTaskFor(item, TVAElementType.ITEM));
        
        verify(service).uploadContent(series);
    }

    @Test
    public void testResolvesAndUploadsBrandIfReferentialIntegrityErrorGivenForItemWithBrandButNoSeries() {
        TransactionReportType report = createReportWithRefError("Brand", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Item item = createItem();
        Brand brand = createBrand();
        
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.<Series>absent());
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.of(brand));
        
        
        handler.handle(report, createTaskFor(item, TVAElementType.ITEM));
        
        verify(service).uploadContent(brand);
    }

    @Test
    public void testResolvesAndUploadsSeriesIfReferentialIntegrityErrorGivenForItemWithSeriesAndBrand() {
        TransactionReportType report = createReportWithRefError("Series", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Item item = createItem();
        Series series = createSeries();
        Brand brand = createBrand();
        
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.of(series));
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.of(brand));
        
        
        handler.handle(report, createTaskFor(item, TVAElementType.ITEM));
        
        verify(service).uploadContent(series);
    }
    
    @Test
    public void testResolvesAndUploadsItemIfReferentialIntegrityErrorGivenForVersion() {
        TransactionReportType report = createReportWithRefError("Episode", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Item item = createItem();
        
        handler.handle(report, createTaskFor(item, TVAElementType.VERSION));
        
        verify(service).uploadContent(item);
    }
    
    @Test
    public void testResolvesItemAndUploadsAppropriateVersionIfReferentialIntegrityErrorGivenForOnDemand() {
        String versionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", versionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn(versionCrid);
        
        handler.handle(report, createTaskFor(item, TVAElementType.ONDEMAND));
        
        verify(service).uploadVersion(new ItemAndVersion(item, version), versionCrid);
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfReferentialIntegrityErrorGivenForOnDemandAndUnableToExtractVersionId() {
        String invalidVersionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/misc_junk/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", invalidVersionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn(invalidVersionCrid);
        
        handler.handle(report, createTaskFor(item, TVAElementType.ONDEMAND));
    }
    
    @Test(expected = RuntimeException.class)
    public void testThrowsIfReferentialIntegrityErrorGivenForOnDemandAndUnableToMatchVersionIdToExpandedVersions() {
        String versionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", versionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn("crid://nitro.bbc.co.uk/iplayer/youview/anotherversioncrid");
        
        handler.handle(report, createTaskFor(item, TVAElementType.ONDEMAND));
    }

    @Test
    public void testResolvesItemAndUploadsAppropriateVersionIfReferentialIntegrityErrorGivenForBroadcast() {
        String versionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", versionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn(versionCrid);
        
        handler.handle(report, createTaskFor(item, TVAElementType.BROADCAST));
        
        verify(service).uploadVersion(new ItemAndVersion(item, version), versionCrid);
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfReferentialIntegrityErrorGivenForBroadcastAndUnableToExtractVersionId() {
        String invalidVersionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/misc_junk/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", invalidVersionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn(invalidVersionCrid);
        
        handler.handle(report, createTaskFor(item, TVAElementType.BROADCAST));
    }
    
    @Test(expected = RuntimeException.class)
    public void testThrowsIfReferentialIntegrityErrorGivenForBroadcastAndUnableToMatchVersionIdToExpandedVersions() {
        String versionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", versionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn("crid://nitro.bbc.co.uk/iplayer/youview/anotherversioncrid");
        
        handler.handle(report, createTaskFor(item, TVAElementType.BROADCAST));
    }

    private Version createVersion() {
        return new Version();
    }

    private Item createItem() {
        Item item = new Film("film", "curie", Publisher.METABROADCAST);
        resolveFromUri(item);
        return item;
    }

    private Series createSeries() {
        Series series = new Series("series", "curie", Publisher.METABROADCAST);
        resolveFromUri(series);
        return series;
    }

    private Brand createBrand() {
        Brand brand = new Brand("brand", "curie", Publisher.METABROADCAST);
        resolveFromUri(brand);
        return brand;
    }

    private void resolveFromUri(Content content) {
        ResolvedContent resolved = new ResolvedContent(ImmutableMap.of(content.getCanonicalUri(), Maybe.<Identified>just(content)));
        when(contentResolver.findByCanonicalUris(ImmutableList.of(content.getCanonicalUri()))).thenReturn(resolved);
    }

    private TransactionReportType createReportWithRefError(String elementType, String elementId) {
        TransactionReportType report = new TransactionReportType();
        
        FragmentReportType fragmentReport = new FragmentReportType();
        
        ControlledMessageType message = new ControlledMessageType();
        
        TextualType comment = new TextualType();
        
        comment.setValue(String.format(REFERENTIAL_INTEGRITY_ERROR_PATTERN, elementType, elementId));
        
        message.setComment(comment);
        message.setReasonCode(REFERENTIAL_INTEGRITY_ERROR_REASON_CODE);
        
        fragmentReport.getMessage().add(message);
        fragmentReport.setSuccess(false);
        
        report.getFragmentUpdateReport().add(fragmentReport);
        report.setState(TransactionStateType.QUARANTINED);
        
        return report;
    }

    private Task createTaskFor(Content content, TVAElementType type) {
        return Task.builder()
                .withAction(Action.UPDATE)
                .withContent(content.getCanonicalUri())
                .withPublisher(content.getPublisher())
                .withStatus(Status.ACCEPTED)
                .withElementType(type)
                .withElementId("brand_prefix" + content.getCanonicalUri())
                .build();
    }

}
