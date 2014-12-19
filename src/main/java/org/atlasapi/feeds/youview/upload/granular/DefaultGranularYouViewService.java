package org.atlasapi.feeds.youview.upload.granular;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.tvanytime.TvaGenerationException;
import org.atlasapi.feeds.tvanytime.granular.GranularTvAnytimeGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.persistence.SentBroadcastEventProgramUrlStore;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.upload.ResultHandler;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.YouViewResult;
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


public class DefaultGranularYouViewService implements GranularYouViewService {
    
    private final Logger log = LoggerFactory.getLogger(DefaultGranularYouViewService.class);
    
    private final GranularTvAnytimeGenerator generator;
    // TODO if content upload takes a contentcrid, then can remove this too
    private final IdGenerator idGenerator;
    private final YouViewRemoteClient client;
    // TODO this will be moved out once task creation happens separately
    private final TaskStore taskStore;
    // TODO move this into decorating youview service?
    private final SentBroadcastEventProgramUrlStore sentBroadcastProgramUrlStore;
    private final ResultHandler resultHandler;
    
    public DefaultGranularYouViewService(GranularTvAnytimeGenerator generator, IdGenerator idGenerator, 
            YouViewRemoteClient client, TaskStore taskStore, 
            SentBroadcastEventProgramUrlStore sentBroadcastProgramUrlStore, ResultHandler resultHandler) {
        this.sentBroadcastProgramUrlStore = checkNotNull(sentBroadcastProgramUrlStore);
        this.generator = checkNotNull(generator);
        this.idGenerator = checkNotNull(idGenerator);
        this.client = checkNotNull(client);
        this.taskStore = checkNotNull(taskStore);
        this.resultHandler = checkNotNull(resultHandler);
    }

    @Override
    public void checkRemoteStatusOf(Task task) {
        if (!task.remoteId().isPresent()) {
            throw new RuntimeException("attempted to check remote status for task " + task.id() + " with no remote id");
        }
        resultHandler.handleRemoteCheckResult(task, client.checkRemoteStatusOf(task.remoteId().get()));
    }

    @Override
    public void uploadContent(Content content) {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateContentTVAFrom(content);

            TVAElementType type = determineType(content);
            Task task = createTaskFor(content, Action.UPDATE, type, idGenerator.generateContentCrid(content));
            resultHandler.handleTransactionResult(task, client.upload(tvaElem));
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
        if (content instanceof Item) {
            return TVAElementType.ITEM;
        }
        throw new RuntimeException("Content " + content.getCanonicalUri() + " of unexpected type. Expected Brand/Series/Item");
    }

    @Override
    public void uploadVersion(ItemAndVersion versionHierarchy, String versionCrid) {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateVersionTVAFrom(versionHierarchy, versionCrid);

            Task task = createTaskFor(versionHierarchy.item(), Action.UPDATE, TVAElementType.VERSION, versionCrid);
            resultHandler.handleTransactionResult(task, client.upload(tvaElem));
        } catch (TvaGenerationException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void uploadBroadcast(ItemBroadcastHierarchy broadcastHierarchy, String broadcastImi) {
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
            resultHandler.handleTransactionResult(task, uploadResult);
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
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateOnDemandTVAFrom(onDemandHierarchy, onDemandImi);

            Task task = createTaskFor(onDemandHierarchy.item(), Action.UPDATE, TVAElementType.ONDEMAND, onDemandImi);

            resultHandler.handleTransactionResult(task, client.upload(tvaElem));
        } catch (TvaGenerationException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void sendDeleteFor(Content content, TVAElementType type, String elementId) {
        Task task = createTaskFor(content, Action.DELETE, type, elementId);
        resultHandler.handleTransactionResult(task, client.sendDeleteFor(elementId));
    }
}
