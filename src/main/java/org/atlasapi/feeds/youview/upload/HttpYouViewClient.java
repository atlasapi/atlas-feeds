package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.metadata._2010.TVAMainType;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.Clock;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;


public class HttpYouViewClient implements YouViewClient {
    
    private final Logger log = LoggerFactory.getLogger(HttpYouViewClient.class);
    
    private final TvAnytimeGenerator generator;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final RevokedContentStore revocationStore;
    private final YouViewRemoteClient client;
    private final TaskStore taskStore;
    private final BroadcastHierarchyExpander broadcastHierarchyExpander;
    private final OnDemandHierarchyExpander onDemandHierarchyExpander;
    
    public HttpYouViewClient(TvAnytimeGenerator generator, IdGenerator idGenerator, 
            Clock clock, RevokedContentStore revocationStore, YouViewRemoteClient client,
            TaskStore taskStore, BroadcastHierarchyExpander broadcastHierarchyExpander, 
            OnDemandHierarchyExpander onDemandHierarchyExpander) {
        this.generator = checkNotNull(generator);
        this.idGenerator = checkNotNull(idGenerator);
        this.clock = checkNotNull(clock);
        this.revocationStore = checkNotNull(revocationStore);
        this.client = checkNotNull(client);
        this.taskStore = checkNotNull(taskStore);
        this.broadcastHierarchyExpander = checkNotNull(broadcastHierarchyExpander);
        this.onDemandHierarchyExpander = checkNotNull(onDemandHierarchyExpander);
    }
    
    /**
     * Given a piece of {@link Content}, generates YouView TVAnytime XML and uploads it to YouView.
     * @param content the content to upload
     */
    @Override
    public void upload(Content content) {
        if (isRevoked(content)) {
            return;
        }
        JAXBElement<TVAMainType> tvaElem = generator.generateTVAnytimeFrom(content);
        
        Task task = createTaskFor(content, Action.UPDATE);
        
        YouViewResult uploadResult = client.upload(tvaElem);
        processResult(task, content, uploadResult);
    }
    
    private boolean isRevoked(Content content) {
        return revocationStore.isRevoked(content.getCanonicalUri());
    }

    private void processResult(Task task, Content content, YouViewResult uploadResult) {
        if (uploadResult.isSuccess()) {
            taskStore.updateWithRemoteId(task.id(), Status.ACCEPTED, uploadResult.result(), clock.now());
        } else {
            Response response = new Response(Status.REJECTED, uploadResult.result(), clock.now());
            taskStore.updateWithResponse(task.id(), response);
        }
    }

    private Task createTaskFor(Content content, Action action) {
        Task task = Task.builder()
                .withAction(action)
                .withContent(content.getCanonicalUri())
                .withPublisher(content.getPublisher())
                .withStatus(Status.NEW)
                .build();
        
        return taskStore.save(task);
    }
    
    @Override
    public void sendDeleteFor(Content content) {
        if (content instanceof Item) {
            sendDelete((Item) content);
        } else {
            sendDelete(content);
        }
    }
    
    // TODO this is a bit of a mess
    // TODO this will represent an Item's hierarchical deletes as a series 
    // of deletes of the same item (no representation of the crid deleted yet)
    private void sendDelete(final Item item) {
        Task task = createTaskFor(item, Action.DELETE);
        YouViewResult deleteResult = client.sendDeleteFor(idGenerator.generateContentCrid(item));
        processDeletionResult(task, item, deleteResult);
        
        Set<String> versionIds = generateVersionIds(item);
        
        for (String versionId: versionIds) {
            task = createTaskFor(item, Action.DELETE);
            deleteResult = client.sendDeleteFor(versionId);
            // need to ideally mark it with type of deleted fragment
            processDeletionResult(task, item, deleteResult);
        }
        
        Set<String> onDemandIds = onDemandHierarchyExpander.expandHierarchy(item).keySet();
        
        for (String onDemandId: onDemandIds) {
            task = createTaskFor(item, Action.DELETE);
            deleteResult = client.sendDeleteFor(onDemandId);
            // need to ideally mark it with type of deleted fragment
            processDeletionResult(task, item, deleteResult);
        }

        Set<String> broadcastIds = broadcastHierarchyExpander.expandHierarchy(item).keySet();
        
        for (String broadcastId: broadcastIds) {
            task = createTaskFor(item, Action.DELETE);
            deleteResult = client.sendDeleteFor(broadcastId);
            // need to ideally mark it with type of deleted fragment
            processDeletionResult(task, item, deleteResult);
        }
    }

    private Set<String> generateVersionIds(final Item item) {
        Set<String> versionIds = ImmutableSet.copyOf(Iterables.transform(item.getVersions(), new Function<Version, String>() {
            @Override
            public String apply(Version input) {
                return idGenerator.generateVersionCrid(item, input);
            }
        }));
        return versionIds;
    }
    
    private void sendDelete(Content content) {
        // TODO this may be too naive - does this need to send deletes for content + all children (e.g. series, episodes for brand)?
        Task task = createTaskFor(content, Action.DELETE);
        YouViewResult deleteResult = client.sendDeleteFor(idGenerator.generateContentCrid(content));
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
    
    @Override
    public void checkRemoteStatusOf(Task task) {
        if (!task.remoteId().isPresent()) {
            log.error("attempted to check remote status for task {} with no remote id", task.id());
        }
        YouViewResult result = client.checkRemoteStatusOf(task.remoteId().get());
        processResult(task, result);
    }

    private void processResult(Task task, YouViewResult result) {
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

        for (String onDemandId: onDemandIds) {
            Task task = createTaskFor(item, Action.DELETE);
            YouViewResult deleteResult = client.sendDeleteFor(onDemandId);
            // need to ideally mark it with type of deleted fragment
            processDeletionResult(task, item, deleteResult);
        }

        revocationStore.revoke(content.getCanonicalUri());
    }

    @Override
    public void unrevoke(Content content) {
        // TODO not quite granular enough, but it'll do
        upload(content);
        revocationStore.unrevoke(content.getCanonicalUri());
    }
}
