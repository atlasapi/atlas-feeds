package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.tvanytime.granular.GranularBroadcastEventGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.metadata._2010.AVAttributesType;
import tva.metadata._2010.AspectRatioType;
import tva.metadata._2010.AudioAttributesType;
import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.VideoAttributesType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class NitroBroadcastEventGenerator implements GranularBroadcastEventGenerator {

    private static final String DEFAULT_ASPECT_RATIO = "16:9";
    private static final String MIX_TYPE_STEREO = "urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3";
    private static final String BROADCAST_AUTHORITY = "pcrid.dmol.co.uk";
    private static final String BROADCAST_PID_AUTHORITY = "bpid.bbc.co.uk";
    private static final String SERVICE_ID_PREFIX = "http://nitro.bbc.co.uk/services/";
    private final IdGenerator idGenerator;
    private static final String TERRESTRIAL_EVENT_LOCATOR_NS = "bbc:terrestrial_event_locator:teleview";
    private static final String TERRESTRIAL_PROGRAMME_CRID_NS = "bbc:terrestrial_programme_crid:teleview";
    
    private final Logger log = LoggerFactory.getLogger(NitroBroadcastEventGenerator.class);

    public NitroBroadcastEventGenerator(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    @Override
    public final BroadcastEventType generate(ItemBroadcastHierarchy hierarchy, String imi) {
        BroadcastEventType broadcastEvent = new BroadcastEventType();
        
        broadcastEvent.setServiceIDRef(serviceIdRefFrom(hierarchy.youViewServiceId()));
        broadcastEvent.setProgram(createProgram(hierarchy.item(), hierarchy.version()));

        Optional<Alias> terrestrialEventLocator = terrestrialEventLocator(hierarchy.broadcast());

        if (terrestrialEventLocator.isPresent()) {
            broadcastEvent.setProgramURL(terrestrialEventLocator.get().getValue());
        }

        broadcastEvent.setInstanceMetadataId(imi);
        broadcastEvent.setInstanceDescription(instanceDescriptionFrom(hierarchy.broadcast()));
        broadcastEvent.setPublishedStartTime(startTimeFrom(hierarchy.broadcast()));
        broadcastEvent.setPublishedDuration(durationFrom(hierarchy.broadcast()));
        broadcastEvent.setFree(TvAnytimeElementFactory.flag(true));
        
        
        return broadcastEvent;
    }

    private Optional<Alias> terrestrialEventLocator(Broadcast broadcast) {
        return aliasWithNamespace(broadcast.getAliases(), TERRESTRIAL_EVENT_LOCATOR_NS);
    }

    private Optional<Alias> aliasWithNamespace(Set<Alias> aliases, final String aliasNamespace) {
        return Iterables.tryFind(aliases, new Predicate<Alias>() {
            @Override
            public boolean apply(Alias input) {
                return aliasNamespace.equals(input.getNamespace());
            }
        });
    }

    private String serviceIdRefFrom(String youViewServiceId) {
        return SERVICE_ID_PREFIX + youViewServiceId;
    }

    private InstanceDescriptionType instanceDescriptionFrom(Broadcast broadcast) {
        InstanceDescriptionType description = new InstanceDescriptionType();
        
        Optional<UniqueIDType> pCridIdentifier = createTerrestrialProgrammeCridIdentifier(broadcast);
        if (pCridIdentifier.isPresent()) {
            description.getOtherIdentifier().add(pCridIdentifier.get());
        }
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

    private Optional<UniqueIDType> createTerrestrialProgrammeCridIdentifier(Broadcast broadcast) {
        UniqueIDType otherId = new UniqueIDType();
        otherId.setAuthority(BROADCAST_AUTHORITY);

        Optional<Alias> alias = aliasWithNamespace(broadcast.getAliases(), TERRESTRIAL_PROGRAMME_CRID_NS);

        if (!alias.isPresent()) {
            log.trace("Terrestrial Programme Crid Identifier not present for broadcast " + broadcast.getCanonicalUri());
            return Optional.absent();
        }

        otherId.setValue(alias.get().getValue());
        return Optional.of(otherId);
    }

    private UniqueIDType createBroadcastPidIdentifier(Broadcast broadcast) {
        UniqueIDType broadcastPidId = new UniqueIDType();
        broadcastPidId.setAuthority(BROADCAST_PID_AUTHORITY);
        broadcastPidId.setValue(broadcast.getSourceId().replace("bbc:", ""));
        return broadcastPidId;
    }
    
    private XMLGregorianCalendar startTimeFrom(Broadcast broadcast) {
        return TvAnytimeElementFactory.gregorianCalendar(broadcast.getTransmissionTime());
    }
    
    private javax.xml.datatype.Duration durationFrom(Broadcast broadcast) {
        return TvAnytimeElementFactory.durationFrom(Duration.standardSeconds(broadcast.getBroadcastDuration()));
    }
    
    private CRIDRefType createProgram(Item item, Version version) {
        CRIDRefType program = new CRIDRefType();
        program.setCrid(idGenerator.generateVersionCrid(item, version));
        return program;
    }
}
