package org.atlasapi.feeds.tasks.youview.creation;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;


public class OnDemandBackpopulationTaskCreator extends TaskCreationTask {

    private final DateTime goLiveEpoch;
    
    public OnDemandBackpopulationTaskCreator(DateTime goLiveEpoch) {
        super();
        this.goLiveEpoch = checkNotNull(goLiveEpoch);
    }
    
    @Override
    protected boolean shouldProcess(Content content) {
        
        if (! (content instanceof Item)) {
            return false;
        }
        Item item = (Item) content;
        
        if (!hasAvailableOnDemandsNotUploadedToYouView(item)) {
            return false;
        }
        
        if (item.getThisOrChildLastUpdated() > EPOCH) {
            uploadItem();
        }
        
        uploadOnDemands
    }
    
    @Override
    protected Predicate<ItemAndVersion> versionFilter(DateTime updatedSince) {
        
    }
    
    @Override 
    protected Predicate<ItemOnDemandHierarchy> onDemandFilter(DateTime updatedSince) {
        
    }
}
