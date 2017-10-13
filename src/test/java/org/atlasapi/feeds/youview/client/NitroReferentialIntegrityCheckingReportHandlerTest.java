package org.atlasapi.feeds.youview.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.creation.TaskCreator;
import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
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
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.base.Maybe;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;

import tva.mpeg7._2008.TextualType;


public class NitroReferentialIntegrityCheckingReportHandlerTest {

    private static final String REFERENTIAL_INTEGRITY_ERROR_REASON_CODE = 
            "http://refdata.youview.com/mpeg7cs/YouViewMetadataIngestReasonCS/2010-09-23#semantic-referential_integrity";
    private static final String REFERENTIAL_INTEGRITY_ERROR_PATTERN = 
            "Referenced %s (crid \"%s\") cannot be found";
    private static final Long TASK_ID = 1l;
    
    Task task = mock(Task.class);
    Payload payload = createPayload();
    TaskCreator taskCreator = mock(TaskCreator.class);
    TaskStore taskStore = mock(TaskStore.class);
    PayloadCreator payloadCreator = mock(PayloadCreator.class);
    private ContentResolver contentResolver = mock(ContentResolver.class);
    private IdGenerator idGenerator = mock(IdGenerator.class);
    private VersionHierarchyExpander versionExpander = new VersionHierarchyExpander(idGenerator);
    private ContentHierarchyExtractor hierarchyExtractor = mock(ContentHierarchyExtractor.class);
    
    private final YouViewReportHandler handler = new NitroReferentialIntegrityCheckingReportHandler(
            taskCreator, 
            idGenerator, 
            taskStore, 
            payloadCreator,
            contentResolver, 
            versionExpander, 
            hierarchyExtractor
    );
    
    @Before
    public void setup() {
        when(task.id()).thenReturn(TASK_ID);
        when(taskStore.save(task)).thenReturn(task);
    }
    
    private Payload createPayload() {
        return new Payload("payload", new DateTime());
    }

    @Test
    public void testThrowsIfReferentialIntegrityErrorGivenForBrand() {
        TransactionReportType report = createReportWithRefError("Series", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Brand brand = createBrand();
        
        handler.handle(report, createTaskFor(brand, TVAElementType.BRAND));
        
        verifyZeroInteractions(taskCreator);
        verifyZeroInteractions(taskStore);
        verifyZeroInteractions(payloadCreator);
    }

    @Test
    public void testResolvesAndUploadsBrandIfReferentialIntegrityErrorGivenForSeriesWithBrand() throws PayloadGenerationException {
        String missingCrid = "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid";
        TransactionReportType report = createReportWithRefError("Brand", missingCrid);
        Brand brand = createBrand();
        Series series = createSeries();
        
        when(hierarchyExtractor.brandFor(series)).thenReturn(Optional.of(brand));
        when(idGenerator.generateContentCrid(brand)).thenReturn(missingCrid);
        when(taskCreator.taskFor(missingCrid, brand, payload, Action.UPDATE)).thenReturn(task);
        when(payloadCreator.payloadFrom(missingCrid, brand)).thenReturn(payload);
        
        handler.handle(report, createTaskFor(series, TVAElementType.SERIES));
        
        verify(taskCreator).taskFor(missingCrid, brand, payload, Action.UPDATE);
        verify(taskStore).save(task);
        verify(payloadCreator).payloadFrom(missingCrid, brand);
    }

    @Test
    public void testThrowsIfReferentialIntegrityErrorGivenForSeriesWithoutBrand() {
        TransactionReportType report = createReportWithRefError("Brand", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Series series = createSeries();
        
        when(hierarchyExtractor.brandFor(series)).thenReturn(Optional.<Brand>absent());
        
        handler.handle(report, createTaskFor(series, TVAElementType.SERIES));
        
        verifyZeroInteractions(taskCreator);
        verifyZeroInteractions(taskStore);
        verifyZeroInteractions(payloadCreator);
    }
    
    @Test
    public void testThrowsIfReferentialIntegrityErrorGivenForItemWithoutBrandOrSeries() {
        TransactionReportType report = createReportWithRefError("Series", "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid");
        Item item = createItem();
        
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.<Series>absent());
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.<Brand>absent());
        
        handler.handle(report, createTaskFor(item, TVAElementType.ITEM));
        
        verifyZeroInteractions(taskCreator);
        verifyZeroInteractions(taskStore);
        verifyZeroInteractions(payloadCreator);
    }

    @Test
    public void testResolvesAndUploadsSeriesIfReferentialIntegrityErrorGivenForItemWithSeriesButNoBrand() throws PayloadGenerationException {
        String missingCrid = "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid";
        TransactionReportType report = createReportWithRefError("Series", missingCrid);
        Item item = createItem();
        Series series = createSeries();
        
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.of(series));
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.<Brand>absent());
        when(idGenerator.generateContentCrid(series)).thenReturn(missingCrid);
        when(taskCreator.taskFor(missingCrid, series, payload, Action.UPDATE)).thenReturn(task);
        when(payloadCreator.payloadFrom(missingCrid, series)).thenReturn(payload);

        handler.handle(report, createTaskFor(item, TVAElementType.ITEM));
        
        verify(taskCreator).taskFor(missingCrid, series, payload, Action.UPDATE);
        verify(taskStore).save(task);
        verify(payloadCreator).payloadFrom(missingCrid, series);
    }

    @Test
    public void testResolvesAndUploadsBrandIfReferentialIntegrityErrorGivenForItemWithBrandButNoSeries() throws PayloadGenerationException {
        String missingCrid = "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid";
        TransactionReportType report = createReportWithRefError("Brand", missingCrid);
        Item item = createItem();
        Brand brand = createBrand();
        
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.<Series>absent());
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.of(brand));
        when(idGenerator.generateContentCrid(brand)).thenReturn(missingCrid);
        when(taskCreator.taskFor(missingCrid, brand, payload, Action.UPDATE)).thenReturn(task);
        when(payloadCreator.payloadFrom(missingCrid, brand)).thenReturn(payload);

        handler.handle(report, createTaskFor(item, TVAElementType.ITEM));

        verify(taskCreator).taskFor(missingCrid, brand, payload, Action.UPDATE);
        verify(taskStore).save(task);
        verify(payloadCreator).payloadFrom(missingCrid, brand);
    }

    @Test
    public void testResolvesAndUploadsSeriesIfReferentialIntegrityErrorGivenForItemWithSeriesAndBrand() throws PayloadGenerationException {
        String missingCrid = "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid";
        TransactionReportType report = createReportWithRefError("Series", missingCrid);
        Item item = createItem();
        Series series = createSeries();
        Brand brand = createBrand();
        
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.of(series));
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.of(brand));
        when(idGenerator.generateContentCrid(series)).thenReturn(missingCrid);
        when(taskCreator.taskFor(missingCrid, series, payload, Action.UPDATE)).thenReturn(task);
        when(payloadCreator.payloadFrom(missingCrid, series)).thenReturn(payload);
        
        
        handler.handle(report, createTaskFor(item, TVAElementType.ITEM));
        
        verify(taskCreator).taskFor(missingCrid, series, payload, Action.UPDATE);
        verify(taskStore).save(task);
        verify(payloadCreator).payloadFrom(missingCrid, series);
    }
    
    @Test
    public void testResolvesAndUploadsItemIfReferentialIntegrityErrorGivenForVersion() throws PayloadGenerationException {
        String missingCrid = "crid://nitro.bbc.co.uk/iplayer/youview/a-missing-crid";
        TransactionReportType report = createReportWithRefError("Episode", missingCrid);
        Item item = createItem();
        
        when(idGenerator.generateContentCrid(item)).thenReturn(missingCrid);
        when(taskCreator.taskFor(missingCrid, item, payload, Action.UPDATE)).thenReturn(task);
        when(payloadCreator.payloadFrom(missingCrid, item)).thenReturn(payload);
        
        handler.handle(report, createTaskFor(item, TVAElementType.VERSION));
        
        verify(taskCreator).taskFor(missingCrid, item, payload, Action.UPDATE);
        verify(taskStore).save(task);
        verify(payloadCreator).payloadFrom(missingCrid, item);
    }
    
    @Test
    public void testResolvesItemAndUploadsAppropriateVersionIfReferentialIntegrityErrorGivenForOnDemand() throws PayloadGenerationException {
        String versionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", versionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        returnContentFromResolver(item);
        when(idGenerator.generateVersionCrid(item, version)).thenReturn(versionCrid);
        ItemAndVersion versionHierarchy = versionExpander.expandHierarchy(item).get(versionCrid);
        when(taskCreator.taskFor(versionCrid, versionHierarchy, payload, Action.UPDATE)).thenReturn(task);
        when(payloadCreator.payloadFrom(versionCrid, versionHierarchy)).thenReturn(payload);
        
        handler.handle(report, createTaskFor(item, TVAElementType.ONDEMAND));
        
        verify(taskCreator).taskFor(versionCrid, versionHierarchy, payload, Action.UPDATE);
        verify(taskStore).save(task);
        verify(payloadCreator).payloadFrom(versionCrid, versionHierarchy);
    }

    @Test
    public void testThrowsIfReferentialIntegrityErrorGivenForOnDemandAndUnableToExtractVersionId() {
        String invalidVersionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/misc_junk/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", invalidVersionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn(invalidVersionCrid);
        
        handler.handle(report, createTaskFor(item, TVAElementType.ONDEMAND));
        
        verifyZeroInteractions(taskCreator);
        verifyZeroInteractions(taskStore);
        verifyZeroInteractions(payloadCreator);
    }
    
    @Test
    public void testThrowsIfReferentialIntegrityErrorGivenForOnDemandAndUnableToMatchVersionIdToExpandedVersions() {
        String versionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", versionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn("crid://nitro.bbc.co.uk/iplayer/youview/anotherversioncrid");
        
        handler.handle(report, createTaskFor(item, TVAElementType.ONDEMAND));
        
        verifyZeroInteractions(taskCreator);
        verifyZeroInteractions(taskStore);
        verifyZeroInteractions(payloadCreator);
    }

    @Test
    public void testResolvesItemAndUploadsAppropriateVersionIfReferentialIntegrityErrorGivenForBroadcast() throws PayloadGenerationException {
        String versionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", versionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        returnContentFromResolver(item);
        when(idGenerator.generateVersionCrid(item, version)).thenReturn(versionCrid);
        ItemAndVersion versionHierarchy = versionExpander.expandHierarchy(item).get(versionCrid);
        when(taskCreator.taskFor(versionCrid, versionHierarchy, payload, Action.UPDATE)).thenReturn(task);
        when(payloadCreator.payloadFrom(versionCrid, versionHierarchy)).thenReturn(payload);
        
        handler.handle(report, createTaskFor(item, TVAElementType.BROADCAST));
        
        
        verify(taskCreator).taskFor(versionCrid, versionHierarchy, payload, Action.UPDATE);
        verify(taskStore).save(task);
        verify(payloadCreator).payloadFrom(versionCrid, versionHierarchy);
    }

    private void returnContentFromResolver(Item item) {
        ResolvedContent resolved = new ResolvedContent(ImmutableMap.of(item.getCanonicalUri(), Maybe.<Identified>just(item)));
        when(contentResolver.findByCanonicalUris(ImmutableList.of(item.getCanonicalUri()))).thenReturn(resolved);
    }

    @Test
    public void testThrowsIfReferentialIntegrityErrorGivenForBroadcastAndUnableToExtractVersionId() {
        String invalidVersionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/misc_junk/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", invalidVersionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn(invalidVersionCrid);
        
        handler.handle(report, createTaskFor(item, TVAElementType.BROADCAST));

        verifyZeroInteractions(taskCreator);
        verifyZeroInteractions(taskStore);
        verifyZeroInteractions(payloadCreator);
    }
    
    @Test
    public void testThrowsIfReferentialIntegrityErrorGivenForBroadcastAndUnableToMatchVersionIdToExpandedVersions() {
        String versionCrid = "crid://nitro.bbc.co.uk/iplayer/youview/versioncrid";
        TransactionReportType report = createReportWithRefError("Version", versionCrid);
        Item item = createItem();
        Version version = createVersion();
        item.addVersion(version);
        
        when(idGenerator.generateVersionCrid(item, version)).thenReturn("crid://nitro.bbc.co.uk/iplayer/youview/anotherversioncrid");
        
        handler.handle(report, createTaskFor(item, TVAElementType.BROADCAST));
        
        verifyZeroInteractions(taskCreator);
        verifyZeroInteractions(taskStore);
        verifyZeroInteractions(payloadCreator);
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
                .withCreated(new DateTime())
                .withDestination(createDestinationFor(content.getCanonicalUri(), type, "brand_prefix" + content.getCanonicalUri()))
                .withPublisher(content.getPublisher())
                .withStatus(Status.ACCEPTED)
                .build();
    }

    private Destination createDestinationFor(String canonicalUri, TVAElementType type, String elementId) {
        return new YouViewDestination(canonicalUri, type, elementId);
    }

}
