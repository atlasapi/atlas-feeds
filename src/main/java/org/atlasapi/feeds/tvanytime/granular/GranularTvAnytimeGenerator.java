package org.atlasapi.feeds.tvanytime.granular;

import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.tvanytime.TvaGenerationException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Content;

import tva.metadata._2010.TVAMainType;

public interface GranularTvAnytimeGenerator {

    JAXBElement<TVAMainType> generateContentTVAFrom(Content content) throws TvaGenerationException;
    
    JAXBElement<TVAMainType> generateVersionTVAFrom(ItemAndVersion version, String versionCrid) throws TvaGenerationException;
    
    JAXBElement<TVAMainType> generateBroadcastTVAFrom(ItemBroadcastHierarchy broadcast, String broadcastImi) throws TvaGenerationException;
    
    JAXBElement<TVAMainType> generateOnDemandTVAFrom(ItemOnDemandHierarchy onDemand, String onDemandImi) throws TvaGenerationException;

    // These methods are just for the feed controller. superfluous?
    JAXBElement<TVAMainType> generateContentTVAFrom(Content content, Map<String, ItemAndVersion> versions, 
            Map<String, ItemBroadcastHierarchy> broadcasts, Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException;
    
    JAXBElement<TVAMainType> generateVersionTVAFrom(Map<String, ItemAndVersion> versions) throws TvaGenerationException;
    
    JAXBElement<TVAMainType> generateBroadcastTVAFrom(Map<String, ItemBroadcastHierarchy> broadcasts) throws TvaGenerationException;
    
    JAXBElement<TVAMainType> generateOnDemandTVAFrom(Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException;
}
