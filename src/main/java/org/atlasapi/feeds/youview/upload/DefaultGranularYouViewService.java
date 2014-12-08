package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.tvanytime.TvaGenerationException;
import org.atlasapi.feeds.tvanytime.granular.GranularTvAnytimeGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.persistence.MongoSentBroadcastEventProgramUrlStore;
import org.atlasapi.feeds.youview.persistence.SentBroadcastEventProgramUrlStore;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.TVAMainType;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.Clock;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;


public class DefaultGranularYouViewService implements GranularYouViewService {
    
    private final Logger log = LoggerFactory.getLogger(DefaultGranularYouViewService.class);
    
    private final GranularTvAnytimeGenerator generator;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final RevokedContentStore revocationStore;
    private final YouViewRemoteClient client;
    private final TaskStore taskStore;
    private final OnDemandHierarchyExpander onDemandHierarchyExpander;
    private final SentBroadcastEventProgramUrlStore sentBroadcastProgramUrlStore;
    
    public DefaultGranularYouViewService(GranularTvAnytimeGenerator generator, IdGenerator idGenerator, 
            Clock clock, RevokedContentStore revocationStore, YouViewRemoteClient client,
            TaskStore taskStore, OnDemandHierarchyExpander onDemandHierarchyExpander,
            MongoSentBroadcastEventProgramUrlStore sentBroadcastProgramUrlStore) {
        this.sentBroadcastProgramUrlStore = checkNotNull(sentBroadcastProgramUrlStore);
        this.generator = checkNotNull(generator);
        this.idGenerator = checkNotNull(idGenerator);
        this.clock = checkNotNull(clock);
        this.revocationStore = checkNotNull(revocationStore);
        this.client = checkNotNull(client);
        this.taskStore = checkNotNull(taskStore);
        this.onDemandHierarchyExpander = checkNotNull(onDemandHierarchyExpander);
    }
    
    private boolean isRevoked(Content content) {
        return revocationStore.isRevoked(content.getCanonicalUri());
    }

    private void processResult(Task task, YouViewResult uploadResult) {
        if (uploadResult.isSuccess()) {
            taskStore.updateWithRemoteId(task.id(), Status.ACCEPTED, uploadResult.result(), clock.now());
        } else {
            Response response = new Response(Status.REJECTED, uploadResult.result(), clock.now());
            taskStore.updateWithResponse(task.id(), response);
        }
    }

    @Override
    public void checkRemoteStatusOf(Task task) {
        if (!task.remoteId().isPresent()) {
            log.error("attempted to check remote status for task {} with no remote id", task.id());
        }
        YouViewResult result = client.checkRemoteStatusOf(task.remoteId().get());
        processRemoteCheckResult(task, result);
    }

    private void processRemoteCheckResult(Task task, YouViewResult result) {
        Status status;
        if (result.isSuccess()) {
            status = parseStatusFromResult(result.result());
        } else {
            status = Status.REJECTED;
        }
        Response response = new Response(status, result.result(), clock.now());
        taskStore.updateWithResponse(task.id(), response);
    }

    private Status parseStatusFromResult(String result) {
        try {
        JAXBContext context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StatusReport report = (StatusReport) unmarshaller.unmarshal(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
        TransactionReportType txnReport = report.getTransactionReport().get(0);
        
        switch (txnReport.getState()) {
        case ACCEPTED:
            return Status.ACCEPTED;
        case COMMITTED:
            return Status.COMMITTED;
        case COMMITTING:
            return Status.COMMITTING;
        case FAILED:
            return Status.FAILED;
        case PUBLISHED:
            return Status.PUBLISHED;
        case PUBLISHING:
            return Status.PUBLISHING;
        case QUARANTINED:
            return Status.QUARANTINED;
        case VALIDATING:
            return Status.VALIDATING;
        default:
            return Status.UNKNOWN;
        }
        } catch (JAXBException e) {
            // TODO horrible things happened
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void revoke(Content content) {
        if (!(content instanceof Item)) {
            throw new RuntimeException("content " + content.getCanonicalUri() + " not an item, cannot revoke");
        }
        Item item = (Item) content;
        Set<String> onDemandIds = onDemandHierarchyExpander.expandHierarchy(item).keySet();

        for (String onDemandId : onDemandIds) {
            Task task = createTaskFor(content, Action.DELETE, TVAElementType.ONDEMAND, onDemandId);
            YouViewResult deleteResult = client.sendDeleteFor(onDemandId);
            processDeletionResult(task, item, deleteResult);
        }

        revocationStore.revoke(content.getCanonicalUri());
    }

    @Override
    public void unrevoke(Content content) {
        if (!(content instanceof Item)) {
            throw new RuntimeException("content " + content.getCanonicalUri() + " not an item, cannot revoke");
        }
        revocationStore.unrevoke(content.getCanonicalUri());
        
        Item item = (Item) content;
        Map<String, ItemOnDemandHierarchy> onDemandHierarchies = onDemandHierarchyExpander.expandHierarchy(item);
        
        try {
            for (Entry<String, ItemOnDemandHierarchy> onDemandHierarchy : onDemandHierarchies.entrySet()) {
                Task task = createTaskFor(content, Action.DELETE, TVAElementType.ONDEMAND, onDemandHierarchy.getKey());
                ItemOnDemandHierarchy hierarchy = onDemandHierarchy.getValue();
                JAXBElement<TVAMainType> tvaMain = generator.generateOnDemandTVAFrom(hierarchy, onDemandHierarchy.getKey());
                YouViewResult uploadResult = client.upload(tvaMain);
                processDeletionResult(task, item, uploadResult);
            }
        } catch (TvaGenerationException e) {
            throw Throwables.propagate(e);
        }
        
    }

    @Override
    public void uploadContent(Content content) {
        if (isRevoked(content)) {
            log.info("content {} is revoked, not uploading", content.getCanonicalUri());
            return;
        }
        
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateContentTVAFrom(content);

            TVAElementType type = determineType(content);
            Task task = createTaskFor(content, Action.UPDATE, type, idGenerator.generateContentCrid(content));

            YouViewResult uploadResult = client.upload(tvaElem);
            processResult(task, uploadResult);
        } catch (TvaGenerationException e) {
            throw Throwables.propagate(e);
        }
    }

    private Task createTaskFor(Content content, Action action, TVAElementType type,
            String elementId) {
        Task task = Task.builder()
                .withAction(action)
                .withContent(content.getCanonicalUri())
                .withPublisher(content.getPublisher())
                .withElementType(type)
                .withElementId(elementId)
                .withStatus(Status.NEW)
                .build();
        
        return taskStore.save(task);
    }

    private TVAElementType determineType(Content content) {
        if (content instanceof Brand) {
            return TVAElementType.BRAND;
        }
        if (content instanceof Series) {
            return TVAElementType.SERIES;
        }
        // TODO this is crude
        return TVAElementType.ITEM;
    }

    @Override
    public void uploadVersion(ItemAndVersion versionHierarchy, String versionCrid) {
        if (isRevoked(versionHierarchy.item())) {
            log.info("content {} is revoked, not uploading", versionHierarchy.item().getCanonicalUri());
            return;
        }
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateVersionTVAFrom(versionHierarchy, versionCrid);

            Task task = createTaskFor(versionHierarchy.item(), Action.UPDATE, TVAElementType.VERSION, versionCrid);

            YouViewResult uploadResult = client.upload(tvaElem);
            processResult(task, uploadResult);
        } catch (TvaGenerationException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void uploadBroadcast(ItemBroadcastHierarchy broadcastHierarchy, String broadcastImi) {
        if (isRevoked(broadcastHierarchy.item())) {
            log.info("content {} is revoked, not uploading", broadcastHierarchy.item().getCanonicalUri());
            return;
        }
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi);

            if (alreadyUploaded(tvaElem)) {
                log.trace("Not uploading broadcast, since its ProgramURL has already been associated with this service ID and item");
                return;
            }
            
            Task task = createTaskFor(broadcastHierarchy.item(), Action.UPDATE, TVAElementType.BROADCAST, broadcastImi);

            YouViewResult uploadResult = client.upload(tvaElem);
            
            if (uploadResult.isSuccess()) {
                recordUpload(tvaElem);
            }
            processResult(task, uploadResult);
        } catch (TvaGenerationException e) {
            throw Throwables.propagate(e);
        }
    }
        
    private void recordUpload(JAXBElement<TVAMainType> tvaElem) {
        BroadcastEventType broadcastEvent = Iterables.getOnlyElement(tvaElem.getValue().getProgramDescription().getProgramLocationTable().getBroadcastEvent());
        String programUrl = broadcastEvent.getProgramURL();
        String serviceIdRef = broadcastEvent.getServiceIDRef();
        String crid = broadcastEvent.getProgram().getCrid();
        sentBroadcastProgramUrlStore.recordSent(crid, programUrl, serviceIdRef);
    }

    private boolean alreadyUploaded(JAXBElement<TVAMainType> tvaElem) {
        BroadcastEventType broadcastEvent = Iterables.getOnlyElement(tvaElem.getValue().getProgramDescription().getProgramLocationTable().getBroadcastEvent());
        String programUrl = broadcastEvent.getProgramURL();
        String serviceIdRef = broadcastEvent.getServiceIDRef();
        String crid = broadcastEvent.getProgram().getCrid();
        return sentBroadcastProgramUrlStore.beenSent(crid, programUrl, serviceIdRef);
    }

    @Override
    public void uploadOnDemand(ItemOnDemandHierarchy onDemandHierarchy, String onDemandImi) {
        if (isRevoked(onDemandHierarchy.item())) {
            log.info("content {} is revoked, not uploading", onDemandHierarchy.item().getCanonicalUri());
            return;
        }
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateOnDemandTVAFrom(onDemandHierarchy, onDemandImi);

            Task task = createTaskFor(onDemandHierarchy.item(), Action.UPDATE, TVAElementType.ONDEMAND, onDemandImi);

            YouViewResult uploadResult = client.upload(tvaElem);
            processResult(task, uploadResult);
        } catch (TvaGenerationException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void sendDeleteFor(Content content, TVAElementType type, String elementId) {
        Task task = createTaskFor(content, Action.DELETE, type, elementId);
        YouViewResult deleteResult = client.sendDeleteFor(elementId);
        processDeletionResult(task, content, deleteResult);
    }

    private void processDeletionResult(Task task, Content content, YouViewResult deleteResult) {
        if (deleteResult.isSuccess()) {
            taskStore.updateWithRemoteId(task.id(), Status.ACCEPTED, deleteResult.result(), clock.now());
        } else {
            Response response = new Response(Status.REJECTED, deleteResult.result(), clock.now());
            taskStore.updateWithResponse(task.id(), response);
        }
    }
}
