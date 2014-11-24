package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.List;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.AbstractOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.AVAttributesType;
import tva.metadata._2010.AspectRatioType;
import tva.metadata._2010.AudioAttributesType;
import tva.metadata._2010.BitRateType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.FlagType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.VideoAttributesType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;

public class NitroOnDemandLocationGenerator extends AbstractOnDemandLocationGenerator {

    private static final String ASPECT_RATIO = "16:9";
//    private static final String YOUVIEW_SERVICE = "http://bbc.co.uk/services/youview";
    private static final String DEV_YOUVIEW_SERVICE = "http://bbc.couk/services/youview";
    private static final String MIX_TYPE_STEREO = "urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3";
    private static final String YOUVIEW_GENRE_MEDIA_AVAILABLE = "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available";
    private static final String GENRE_TYPE_OTHER = "other";
    private static final Integer DEFAULT_BIT_RATE = 3200000;
    private static final Integer DEFAULT_HORIZONTAL_SIZE = 1280;
    private static final Integer DEFAULT_VERTICAL_SIZE = 720;
    private static final Integer DEFAULT_DURATION = 30 * 60;
    private static final String BROADCAST_AUTHORITY = "www.bbc.co.uk";
    private static final String DEFAULT_ON_DEMAND_PIPS_ID = "b00gszl0.imi:bbc.co.uk/pips/65751802";

    private final TvAnytimeElementFactory elementFactory;

    public NitroOnDemandLocationGenerator(IdGenerator idGenerator, TvAnytimeElementFactory elementFactory) {
        super(idGenerator);
        this.elementFactory = checkNotNull(elementFactory);
    }
    
    @Override
    public OnDemandProgramType generate(String imi, Item item, Version version, Encoding encoding, Location location) {
        
        ExtendedOnDemandProgramType onDemand = new ExtendedOnDemandProgramType();
        
        // TODO is this a single service?
        onDemand.setServiceIDRef(DEV_YOUVIEW_SERVICE);
        onDemand.setProgram(generateProgram(item, version));
        onDemand.setInstanceMetadataId(imi);
        onDemand.setInstanceDescription(generateInstanceDescription(item, encoding));
        onDemand.setPublishedDuration(generatePublishedDuration(version));
        onDemand.setStartOfAvailability(generateAvailabilityStart(location));
        onDemand.setEndOfAvailability(generateAvailabilityEnd(location));
        onDemand.setFree(generateFree());

        return onDemand;
    }
    
    private FlagType generateFree() {
        FlagType free = new FlagType();
        free.setValue(true);
        return free;
    }

    private InstanceDescriptionType generateInstanceDescription(Item item, Encoding encoding) {
        InstanceDescriptionType instanceDescription = new InstanceDescriptionType();
        
        instanceDescription.getGenre().addAll(generateGenres());
        instanceDescription.setAVAttributes(generateAvAttributes(encoding));
        instanceDescription.getOtherIdentifier().add(createIdentifierFromPipsIdentifier());
        
        return instanceDescription;
    }

    private UniqueIDType createIdentifierFromPipsIdentifier() {
        UniqueIDType otherId = new UniqueIDType();
        otherId.setAuthority(BROADCAST_AUTHORITY);
        // TODO this will need ingesting from NITRO - on-demand id, type = "pips"
        otherId.setValue(DEFAULT_ON_DEMAND_PIPS_ID);
        return otherId;
    }

    private AVAttributesType generateAvAttributes(Encoding encoding) {
        AVAttributesType attributes = new AVAttributesType();

        attributes.getAudioAttributes().add(generateAudioAttributes());
        attributes.setVideoAttributes(generateVideoAttributes());       
        attributes.setBitRate(generateBitRate(encoding));
        
        return attributes;
    }

    private AudioAttributesType generateAudioAttributes() {
        AudioAttributesType attributes = new AudioAttributesType();
        
        ControlledTermType mixType = new ControlledTermType();
        mixType.setHref(MIX_TYPE_STEREO);
        attributes.setMixType(mixType);
        
        return attributes;
    }
    
    private VideoAttributesType generateVideoAttributes() {
        VideoAttributesType videoAttributes = new VideoAttributesType();
        
        videoAttributes.setHorizontalSize(DEFAULT_HORIZONTAL_SIZE);
        videoAttributes.setVerticalSize(DEFAULT_VERTICAL_SIZE);
        
        AspectRatioType aspectRatio = new AspectRatioType();
        aspectRatio.setValue(ASPECT_RATIO);
        
        videoAttributes.getAspectRatio().add(aspectRatio);
        
        return videoAttributes;
    }

    private BitRateType generateBitRate(Encoding encoding) {
        Integer bitRate = encoding.getBitRate();
        if (bitRate == null) {
            // default bit rate
            bitRate = DEFAULT_BIT_RATE;
        }
        BitRateType bitRateType = new BitRateType();
        bitRateType.setVariable(false);
        bitRateType.setValue(BigInteger.valueOf(bitRate));
        return bitRateType;
    }

    private List<GenreType> generateGenres() {
        GenreType mediaAvailable = new GenreType();
        mediaAvailable.setType(GENRE_TYPE_OTHER);
        mediaAvailable.setHref(YOUVIEW_GENRE_MEDIA_AVAILABLE);
        
        return ImmutableList.of(mediaAvailable);
    }

    private Duration generatePublishedDuration(Version version) {
        Integer durationInSecs = version.getDuration();
        if (durationInSecs == null) {
            durationInSecs = durationFromFirstBroadcast(version);
        } 
        return elementFactory.durationFrom(org.joda.time.Duration.standardSeconds(durationInSecs));
    }

    // TODO this is a workaround until versions are ingested correctly from BBC Nitro
    private Integer durationFromFirstBroadcast(Version version) {
        Broadcast broadcast = Iterables.getFirst(version.getBroadcasts(), null);
        if (broadcast == null) {
            // this needs to go away
            return DEFAULT_DURATION;
//            throw new RuntimeException("no broadcasts on version " + version.getCanonicalUri());
        }
        return broadcast.getBroadcastDuration();
    }

    private XMLGregorianCalendar generateAvailabilityStart(Location location) {
        Policy policy = location.getPolicy();
        return elementFactory.gregorianCalendar(policy.getAvailabilityStart());
    }

    private XMLGregorianCalendar generateAvailabilityEnd(Location location) {
        Policy policy = location.getPolicy();
        return elementFactory.gregorianCalendar(policy.getAvailabilityEnd());
    }
}
