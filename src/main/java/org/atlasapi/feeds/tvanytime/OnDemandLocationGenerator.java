package org.atlasapi.feeds.tvanytime;

import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;

import tva.metadata._2010.OnDemandProgramType;

public interface OnDemandLocationGenerator {
    
    OnDemandProgramType generate(ItemOnDemandHierarchy onDemand, String onDemandImi);
}
