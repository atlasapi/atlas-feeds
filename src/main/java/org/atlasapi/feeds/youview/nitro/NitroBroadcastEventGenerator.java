package org.atlasapi.feeds.youview.nitro;

import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.mpeg7._2008.UniqueIDType;

import static com.google.common.base.Preconditions.checkNotNull;

public class NitroBroadcastEventGenerator implements BroadcastEventGenerator {

    private static final Logger log = LoggerFactory.getLogger(NitroBroadcastEventGenerator.class);
    private static final String BROADCAST_AUTHORITY = "pcrid.dmol.co.uk";
    private static final String BROADCAST_PID_AUTHORITY = "bpid.bbc.co.uk";
    private static final String SERVICE_ID_PREFIX = "http://nitro.bbc.co.uk/services/";
    private static final String TERRESTRIAL_EVENT_LOCATOR_NS = "bbc:terrestrial_event_locator:teleview";
    private static final String TERRESTRIAL_PROGRAMME_CRID_NS = "bbc:terrestrial_programme_crid:teleview";

    private final IdGenerator idGenerator;

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

        return description;
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
