package org.atlasapi.feeds.youview.persistence;

import org.atlasapi.media.entity.Broadcast;
import tva.metadata._2010.TVAMainType;

import javax.xml.bind.JAXBElement;

public interface BroadcastEventDeduplicator {

    boolean shouldUpload(JAXBElement<TVAMainType> broadcastEventTva);
    void recordUpload(JAXBElement<TVAMainType> broadcastEventTva, Broadcast broadcast);
}
