package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.services.ServiceMapping;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class NitroBroadcastEventGenerator implements BroadcastEventGenerator {

    private static final String BROADCAST_AUTHORITY = "pcrid.dmol.co.uk";
    private static final String BROADCAST_PID_AUTHORITY = "bpid.bbc.co.uk";
    private static final String BROADCAST_CRID = "crid://fp.bbc.co.uk/SILG5";
    private static final String SERVICE_ID_PREFIX = "http://bbc.co.uk/services/";
    private static final String PROGRAM_URL = "dvb://233A..A020;A876";

    private final Logger log = LoggerFactory.getLogger(NitroBroadcastEventGenerator.class);
    private final IdGenerator idGenerator;
    private final TvAnytimeElementFactory elementFactory;
    private final ServiceMapping serviceMapping;
    private final BbcServiceIdResolver serviceIdResolver;

    public NitroBroadcastEventGenerator(IdGenerator idGenerator, TvAnytimeElementFactory elementFactory,
            ServiceMapping serviceMapping, BbcServiceIdResolver serviceIdResolver) {
        this.idGenerator = checkNotNull(idGenerator);
        this.elementFactory = checkNotNull(elementFactory);
        this.serviceMapping = checkNotNull(serviceMapping);
        this.serviceIdResolver = checkNotNull(serviceIdResolver);
    }
    
    @Override
    public Iterable<BroadcastEventType> generate(Item item) {
        return FluentIterable.from(item.getVersions())
                .transformAndConcat(toBroadcastEventTypes(item));
    }
    
    private Function<Version, Iterable<BroadcastEventType>> toBroadcastEventTypes(final Item item) {
        return new Function<Version, Iterable<BroadcastEventType>>() {
            @Override
            public Iterable<BroadcastEventType> apply(Version input) {
                return toBroadcastEventTypes(item, input);
            }
        };
    }
    
    private Iterable<BroadcastEventType> toBroadcastEventTypes(final Item item, final Version version) {
        return FluentIterable.from(version.getBroadcasts())
                .transformAndConcat(new Function<Broadcast, Iterable<BroadcastEventType>>() {
                    @Override
                    public Iterable<BroadcastEventType> apply(Broadcast input) {
                        return toBroadcastEventTypes(item, version, input);
                    }
                }
        );
    }
    
    private Iterable<BroadcastEventType> toBroadcastEventTypes(final Item item, final Version version, final Broadcast broadcast) {
        Iterable<String> youViewServiceIds = serviceMapping.youviewServiceIdFor(serviceIdResolver.resolveSId(broadcast));
        if (Iterables.isEmpty(youViewServiceIds)) {
            log.warn("broadcast {} on {} has no mapped YouView service IDs", broadcast.getCanonicalUri(), broadcast.getBroadcastOn());
            return ImmutableList.of();
        }
        return Iterables.transform(youViewServiceIds, new Function<String, BroadcastEventType>() {
            @Override
            public BroadcastEventType apply(String input) {
                return toBroadcastEventType(item, version, broadcast, input);
            }
        });
    }
    

    private BroadcastEventType toBroadcastEventType(Item item, Version version, Broadcast broadcast, String youViewServiceId) {
        BroadcastEventType broadcastEvent = new BroadcastEventType();
        
        broadcastEvent.setServiceIDRef(serviceIdRefFrom(youViewServiceId));
        broadcastEvent.setProgram(createProgram(item, version));
        // TODO need to update nitro - ingest id from broadcast - type = "terrestrial_event_locator"
        broadcastEvent.setProgramURL(PROGRAM_URL);
        broadcastEvent.setInstanceMetadataId(idGenerator.generateBroadcastImi(broadcast));
        broadcastEvent.setInstanceDescription(instanceDescriptionFrom(broadcast));
        broadcastEvent.setPublishedStartTime(startTimeFrom(broadcast));
        broadcastEvent.setPublishedDuration(durationFrom(broadcast));
        broadcastEvent.setFree(elementFactory.flag(true));
        
        
        return broadcastEvent;
    }

    private String serviceIdRefFrom(String youViewServiceId) {
        return SERVICE_ID_PREFIX + youViewServiceId;
    }

    private CRIDRefType createProgram(Item item, Version version) {
        CRIDRefType program = new CRIDRefType();
        program.setCrid(idGenerator.generateVersionCrid(item, version));
        return program;
    }
    
    private InstanceDescriptionType instanceDescriptionFrom(Broadcast broadcast) {
        InstanceDescriptionType description = new InstanceDescriptionType();
        
        UniqueIDType otherId = new UniqueIDType();
        otherId.setAuthority(BROADCAST_AUTHORITY);
        // TODO this will need ingesting from NITRO - broadcast id, type = "terrestrial_programme_crid"
        otherId.setValue(BROADCAST_CRID);
        
        description.getOtherIdentifier().add(otherId);
        
        UniqueIDType broadcastPidId = new UniqueIDType();
        broadcastPidId.setAuthority(BROADCAST_PID_AUTHORITY);
        broadcastPidId.setValue(broadcast.getSourceId().replace("bbc:", ""));
        
        description.getOtherIdentifier().add(broadcastPidId);
        return description;
    }
    
    private XMLGregorianCalendar startTimeFrom(Broadcast broadcast) {
        return elementFactory.gregorianCalendar(broadcast.getTransmissionTime());
    }
    
    private javax.xml.datatype.Duration durationFrom(Broadcast broadcast) {
        return elementFactory.durationFrom(Duration.standardSeconds(broadcast.getBroadcastDuration()));
    }
}
