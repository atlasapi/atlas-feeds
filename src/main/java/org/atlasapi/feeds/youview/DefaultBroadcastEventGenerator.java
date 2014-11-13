package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.tvanytime.TVAnytimeElementFactory;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.base.Maybe;

// TODO this is BBC specific, publisher specific code should be extracted to PublisherConfigFactory
// TODO should the channel alias lookup be extracted?
public class DefaultBroadcastEventGenerator implements BroadcastEventGenerator {

    private static final String BROADCAST_AUTHORITY = "pcrid.dmol.co.uk";
    private static final String BROADCAST_PID_AUTHORITY = "bpid.bbc.co.uk";
    private static final String BROADCAST_CRID = "crid://fp.bbc.co.uk/SILG5";
    private static final String SERVICE_ID_REF_PREFIX = "http://bbc.co.uk/services/";
    private static final String PROGRAM_CRID_PREFIX = "crid://bbc.co.uk/iplayer/nitro/youview/";
    private static final String PROGRAM_URL = "dvb://233A..A020;A876";
    private static final String IMI = "imi:www.bbc.co.uk/e290a99f07b8962a503d08f6ca1e8bb3";
    private static final String BBC_SID_NAMESPACE = "bbc:service:id";
    private static final Predicate<Alias> BBC_SID_ALIAS = new Predicate<Alias>() {
        @Override
        public boolean apply(Alias input) {
            return BBC_SID_NAMESPACE.equals(input.getNamespace());
        }
    };
    private final Function<Broadcast, BroadcastEventType> toBroadcastEvent = 
            new Function<Broadcast, BroadcastEventType>() {
                @Override
                public BroadcastEventType apply(Broadcast input) {
                    return toBroadcastEventType(input);
                }
            };
    private final TVAnytimeElementFactory elementFactory;
    private final BroadcastIdGenerator idGenerator;
    private final ChannelResolver channelResolver;

    public DefaultBroadcastEventGenerator(TVAnytimeElementFactory elementFactory, BroadcastIdGenerator idGenerator,
            ChannelResolver channelResolver) {
        this.elementFactory = checkNotNull(elementFactory);
        this.idGenerator = checkNotNull(idGenerator);
        this.channelResolver = checkNotNull(channelResolver);
    }
    
    @Override
    public Iterable<BroadcastEventType> generate(Item item) {
        return FluentIterable.from(item.getVersions())
                .transformAndConcat(toBroadcasts())
                .transform(toBroadcastEventType());
    }
    
    private static Function<Version, Iterable<Broadcast>> toBroadcasts() {
        return new Function<Version, Iterable<Broadcast>>() {
            @Override
            public Iterable<Broadcast> apply(Version input) {
                return input.getBroadcasts();
            }
        };
    }

    private Function<Broadcast, BroadcastEventType> toBroadcastEventType() {
        return toBroadcastEvent;
    }

    private BroadcastEventType toBroadcastEventType(Broadcast broadcast) {
        BroadcastEventType broadcastEvent = new BroadcastEventType();
        
        broadcastEvent.setServiceIDRef(serviceIdRef(broadcast));
        broadcastEvent.setProgram(createProgram(broadcast));
        // TODO need to update nitro - ingest id from broadcast - type = "terrestrial_event_locator"
        broadcastEvent.setProgramURL(PROGRAM_URL);
        // TODO where is this from? appears synthesised, but from what?
        broadcastEvent.setInstanceMetadataId(IMI);
        broadcastEvent.setInstanceDescription(instanceDescriptionFrom(broadcast));
        broadcastEvent.setPublishedStartTime(startTimeFrom(broadcast));
        broadcastEvent.setPublishedDuration(durationFrom(broadcast));
        broadcastEvent.setFree(elementFactory.flag(true));
        
        return broadcastEvent;
    }

    private String serviceIdRef(Broadcast broadcast) {
        // TODO need to determine whether serviceIDRef suffix is static/required
        return SERVICE_ID_REF_PREFIX + resolveServiceId(broadcast.getBroadcastOn());
    }

    private String resolveServiceId(String channelUri) {
        Maybe<Channel> resolved = channelResolver.fromUri(channelUri);
        if (resolved.isNothing()) {
            throw new NoChannelFoundException(channelUri);
        }
        Channel channel = resolved.requireValue();
        Iterable<Alias> bbcSIdAliases = Iterables.filter(channel.getAliases(), BBC_SID_ALIAS);
        if (Iterables.isEmpty(bbcSIdAliases)) {
            throw new NoSuchChannelAliasException(BBC_SID_NAMESPACE);
        }
        Alias sidAlias = Iterables.getOnlyElement(bbcSIdAliases);
        return sidAlias.getValue();
    }

    private CRIDRefType createProgram(Broadcast broadcast) {
        // TODO need a consistent reproducible method to create a unique id for a broadcast
        CRIDRefType program = new CRIDRefType();
        program.setCrid(PROGRAM_CRID_PREFIX + idGenerator.generate(broadcast));
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
        // broadcasts hold duration as an integer number of seconds
        Duration duration = Duration.standardSeconds(broadcast.getBroadcastDuration());
        return elementFactory.durationFrom(duration);
    }
}
