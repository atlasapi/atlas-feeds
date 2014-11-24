package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.AbstractBroadcastEventGenerator;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.services.BroadcastServiceMapping;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;

import tva.metadata._2010.AVAttributesType;
import tva.metadata._2010.AspectRatioType;
import tva.metadata._2010.AudioAttributesType;
import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.VideoAttributesType;
import tva.mpeg7._2008.UniqueIDType;

public class NitroBroadcastEventGenerator extends AbstractBroadcastEventGenerator {

    private static final String DEFAULT_ASPECT_RATIO = "16:9";
    private static final String MIX_TYPE_STEREO = "urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3";
    private static final String BROADCAST_AUTHORITY = "pcrid.dmol.co.uk";
    private static final String BROADCAST_PID_AUTHORITY = "bpid.bbc.co.uk";
    private static final String BROADCAST_CRID = "crid://fp.bbc.co.uk/SILG5";
//    private static final String SERVICE_ID_PREFIX = "http://bbc.co.uk/services/";
    private static final String DEV_SERVICE_ID_PREFIX = "http://bbc.couk/services/";
    private static final String PROGRAM_URL = "dvb://233A..A020;A876";

    private final TvAnytimeElementFactory elementFactory;

    public NitroBroadcastEventGenerator(IdGenerator idGenerator, TvAnytimeElementFactory elementFactory,
            BroadcastServiceMapping serviceMapping, BbcServiceIdResolver serviceIdResolver) {
        super(idGenerator, serviceMapping, serviceIdResolver);
        this.elementFactory = checkNotNull(elementFactory);
    }

    @Override
    public BroadcastEventType generate(String imi, Item item, Version version, Broadcast broadcast, String youViewServiceId) {
        BroadcastEventType broadcastEvent = new BroadcastEventType();
        
        broadcastEvent.setServiceIDRef(serviceIdRefFrom(youViewServiceId));
        broadcastEvent.setProgram(createProgram(item, version));
        // TODO need to update nitro - ingest id from broadcast - type = "terrestrial_event_locator"
        broadcastEvent.setProgramURL(PROGRAM_URL);
        broadcastEvent.setInstanceMetadataId(imi);
        broadcastEvent.setInstanceDescription(instanceDescriptionFrom(broadcast));
        broadcastEvent.setPublishedStartTime(startTimeFrom(broadcast));
        broadcastEvent.setPublishedDuration(durationFrom(broadcast));
        broadcastEvent.setFree(elementFactory.flag(true));
        
        
        return broadcastEvent;
    }

    private String serviceIdRefFrom(String youViewServiceId) {
//        return SERVICE_ID_PREFIX + youViewServiceId;
        return DEV_SERVICE_ID_PREFIX + youViewServiceId;
    }

    private InstanceDescriptionType instanceDescriptionFrom(Broadcast broadcast) {
        InstanceDescriptionType description = new InstanceDescriptionType();
        
        description.getOtherIdentifier().add(createTerrestrialProgrammeCridIdentifier());
        description.getOtherIdentifier().add(createBroadcastPidIdentifier(broadcast));
        description.setAVAttributes(createAVAttributes());
        
        return description;
    }

    private AVAttributesType createAVAttributes() {
        AVAttributesType avAttributes = new AVAttributesType();
        
        avAttributes.getAudioAttributes().add(createAudioAttributes());
        avAttributes.setVideoAttributes(createVideoAttributes());
        
        return avAttributes;
    }

    private VideoAttributesType createVideoAttributes() {
        VideoAttributesType videoAttributes = new VideoAttributesType();
        
        AspectRatioType aspectRatio = new AspectRatioType();
        aspectRatio.setValue(DEFAULT_ASPECT_RATIO);
        
        videoAttributes.getAspectRatio().add(aspectRatio);
        
        return videoAttributes;
    }

    private AudioAttributesType createAudioAttributes() {
        AudioAttributesType audioAttributes = new AudioAttributesType();
        ControlledTermType mixType = new ControlledTermType();
        mixType.setHref(MIX_TYPE_STEREO);
        audioAttributes.setMixType(mixType);
        return audioAttributes;
    }

    private UniqueIDType createTerrestrialProgrammeCridIdentifier() {
        UniqueIDType otherId = new UniqueIDType();
        otherId.setAuthority(BROADCAST_AUTHORITY);
        // TODO this will need ingesting from NITRO - broadcast id, type = "terrestrial_programme_crid"
        otherId.setValue(BROADCAST_CRID);
        return otherId;
    }

    private UniqueIDType createBroadcastPidIdentifier(Broadcast broadcast) {
        UniqueIDType broadcastPidId = new UniqueIDType();
        broadcastPidId.setAuthority(BROADCAST_PID_AUTHORITY);
        broadcastPidId.setValue(broadcast.getSourceId().replace("bbc:", ""));
        return broadcastPidId;
    }
    
    private XMLGregorianCalendar startTimeFrom(Broadcast broadcast) {
        return elementFactory.gregorianCalendar(broadcast.getTransmissionTime());
    }
    
    private javax.xml.datatype.Duration durationFrom(Broadcast broadcast) {
        return elementFactory.durationFrom(Duration.standardSeconds(broadcast.getBroadcastDuration()));
    }
}
