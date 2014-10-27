package org.atlasapi.feeds.tvanytime;

import org.atlasapi.feeds.youview.ContentPermit;
import org.atlasapi.media.entity.Content;

import com.google.common.base.Optional;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.ProgramInformationType;


public interface TVAnytimeElementCreator {

    ContentPermit permit();
    Iterable<GroupInformationType> createGroupInformationElementsFor(Content content);
    Optional<ProgramInformationType> createProgramInformationElementFor(Content content);
    Iterable<OnDemandProgramType> createOnDemandElementsFor(Content content);
    Iterable<BroadcastEventType> createBroadcastEventElementsFor(Content content);
}
