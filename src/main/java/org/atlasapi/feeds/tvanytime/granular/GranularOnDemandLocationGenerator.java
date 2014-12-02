package org.atlasapi.feeds.tvanytime.granular;

import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;

import tva.metadata._2010.OnDemandProgramType;

public interface GranularOnDemandLocationGenerator {
    
    OnDemandProgramType generate(ItemOnDemandHierarchy onDemand, String onDemandImi);
}
