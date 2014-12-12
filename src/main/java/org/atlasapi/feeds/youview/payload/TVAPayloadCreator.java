package org.atlasapi.feeds.youview.payload;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.granular.GranularTvAnytimeGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.media.entity.Content;


public class TVAPayloadCreator implements PayloadCreator {
    
    private final GranularTvAnytimeGenerator generator;
    
    

    @Override
    public Payload createFrom(Content content) {
        
        return new Payload();
    }

    @Override
    public Payload createFrom(String versionCrid, ItemAndVersion versionHierarchy) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Payload createFrom(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Payload createFrom(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy) {
        // TODO Auto-generated method stub
        return null;
    }

}
