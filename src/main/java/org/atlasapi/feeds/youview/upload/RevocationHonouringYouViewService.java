package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RevocationHonouringYouViewService implements GranularYouViewService {

    private final Logger log = LoggerFactory.getLogger(RevocationHonouringYouViewService.class);
    private final RevokedContentStore revocationStore;
    private final GranularYouViewService delegate;
    
    public RevocationHonouringYouViewService(RevokedContentStore revocationStore, GranularYouViewService delegate) {
        this.revocationStore = checkNotNull(revocationStore);
        this.delegate = checkNotNull(delegate);
    }
    
    private boolean isRevoked(Content content) {
        return revocationStore.isRevoked(content.getCanonicalUri());
    }

    @Override
    public void uploadContent(Content content) {
        if (isRevoked(content)) {
            log.info("content {} is revoked, not uploading", content.getCanonicalUri());
            return;
        }
        delegate.uploadContent(content);
    }

    @Override
    public void uploadVersion(ItemAndVersion versionHierarchy, String versionCrid) {
        if (isRevoked(versionHierarchy.item())) {
            log.info("content {} is revoked, not uploading", versionHierarchy.item().getCanonicalUri());
            return;
        }
        delegate.uploadVersion(versionHierarchy, versionCrid);
    }

    @Override
    public void uploadBroadcast(ItemBroadcastHierarchy broadcastHierarchy, String broadcastImi) {
        if (isRevoked(broadcastHierarchy.item())) {
            log.info("content {} is revoked, not uploading", broadcastHierarchy.item().getCanonicalUri());
            return;
        }
        delegate.uploadBroadcast(broadcastHierarchy, broadcastImi);
    }

    @Override
    public void uploadOnDemand(ItemOnDemandHierarchy onDemandHierarchy, String onDemandImi) {
        if (isRevoked(onDemandHierarchy.item())) {
            log.info("content {} is revoked, not uploading", onDemandHierarchy.item().getCanonicalUri());
            return;
        }
        delegate.uploadOnDemand(onDemandHierarchy, onDemandImi);
    }

    @Override
    public void sendDeleteFor(Content content, TVAElementType type, String elementId) {
        if (isRevoked(content)) {
            log.info("content {} is revoked, not uploading", content.getCanonicalUri());
            return;
        }
        delegate.sendDeleteFor(content, type, elementId);
    }

    @Override
    public void checkRemoteStatusOf(Task task) {
        delegate.checkRemoteStatusOf(task);
    }

}
