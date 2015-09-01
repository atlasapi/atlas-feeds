package org.atlasapi.feeds.tvanytime;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.ProgramInformationType;

/**
 * For a given {@link Item}, generates a set of TVAnytime versions, given a 
 * certain scheme for mapping from the Atlas Item + {@link Version} to the YouView
 * version.
 * 
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public interface ProgramInformationGenerator {
    
    ProgramInformationType generate(ItemAndVersion version, String versionCrid);
}
