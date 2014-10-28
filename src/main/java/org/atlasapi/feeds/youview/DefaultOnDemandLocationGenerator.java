package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.YouViewGeneratorUtils.getAsin;

import java.math.BigInteger;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.youview.ids.IdParser;
import org.atlasapi.feeds.youview.ids.PublisherIdUtility;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.AVAttributesType;
import tva.metadata._2010.AspectRatioType;
import tva.metadata._2010.AudioAttributesType;
import tva.metadata._2010.BitRateType;
import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.FlagType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.VideoAttributesType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;

public class DefaultOnDemandLocationGenerator implements OnDemandLocationGenerator {

    private static final String YOUVIEW_MIX_TYPE = "urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3";
    private static final String YOUVIEW_GENRE_SUBSCRIPTION_REQUIRED = "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription";
    private static final String YOUVIEW_GENRE_MEDIA_AVAILABLE = "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available";
    private static final String GENRE_TYPE_OTHER = "other";

    private DatatypeFactory datatypeFactory;
    private final YouViewPerPublisherFactory configFactory;

    /**
     * NB DatatypeFactory is required for creation of javax Durations
     * This DatatypeFactory class may not be threadsafe
     */
    public DefaultOnDemandLocationGenerator(YouViewPerPublisherFactory configFactory) {
        this.configFactory = checkNotNull(configFactory);
        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            Throwables.propagate(e);
        }
    }
    
    @Override
    public Optional<OnDemandProgramType> generate(Item item) {
        Publisher publisher = item.getPublisher();
        PublisherIdUtility config = configFactory.getIdUtil(publisher);
        IdParser idParser = configFactory.getIdParser(publisher);
        
        Version version = Iterables.getOnlyElement(item.getVersions());
        if (version.getManifestedAs().isEmpty()) {
            return Optional.absent();
        }
        
        ExtendedOnDemandProgramType onDemand = new ExtendedOnDemandProgramType();
        
        onDemand.setServiceIDRef(config.getOnDemandServiceId());
        onDemand.setProgram(generateProgram(item));
        onDemand.setInstanceMetadataId(idParser.createImi(config.getImiPrefix(), item));
        onDemand.setInstanceDescription(generateInstanceDescription(item));
        onDemand.setPublishedDuration(generatePublishedDuration(item));
        onDemand.setStartOfAvailability(generateAvailabilityStart(item));
        onDemand.setEndOfAvailability(generateAvailabilityEnd(item));
        onDemand.setFree(generateFree());

        return Optional.<OnDemandProgramType>fromNullable(onDemand);
    }
    
    // hardcoded
    private FlagType generateFree() {
        FlagType free = new FlagType();
        free.setValue(false);
        return free;
    }

    private CRIDRefType generateProgram(Item item) {
        Publisher publisher = item.getPublisher();
        PublisherIdUtility config = configFactory.getIdUtil(publisher);
        IdParser idParser = configFactory.getIdParser(publisher);
                
        CRIDRefType program = new CRIDRefType();
        program.setCrid(idParser.createVersionCrid(config.getCridPrefix(), item));
        return program;
    }

    private InstanceDescriptionType generateInstanceDescription(Item item) {
        InstanceDescriptionType instanceDescription = new InstanceDescriptionType();
        
        instanceDescription.getGenre().addAll(generateGenres());
        instanceDescription.setAVAttributes(generateAvAttributes(item));
        instanceDescription.getOtherIdentifier().add(generateOtherId(item));
        
        return instanceDescription;
    }

    private AVAttributesType generateAvAttributes(Item item) {
        AVAttributesType attributes = new AVAttributesType();

        Version version = Iterables.getOnlyElement(item.getVersions());
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());

        attributes.getAudioAttributes().add(generateAudioAttributes());
        attributes.setVideoAttributes(generateVideoAttributes(encoding));
        attributes.setBitRate(generateBitRate(encoding));
        
        return attributes;
    }

    private AudioAttributesType generateAudioAttributes() {
        AudioAttributesType attributes = new AudioAttributesType();
        ControlledTermType mixType = new ControlledTermType();
        mixType.setHref(YOUVIEW_MIX_TYPE);
        attributes.setMixType(mixType);
        return attributes;
    }

    private VideoAttributesType generateVideoAttributes(Encoding encoding) {
        VideoAttributesType attributes = new VideoAttributesType();

        attributes.setHorizontalSize(encoding.getVideoHorizontalSize());
        attributes.setVerticalSize(encoding.getVideoVerticalSize());
        AspectRatioType aspectRatio = new AspectRatioType();
        aspectRatio.setValue(encoding.getVideoAspectRatio());
        attributes.getAspectRatio().add(aspectRatio);

        return attributes;
    }

    private BitRateType generateBitRate(Encoding encoding) {
        BitRateType bitRate = new BitRateType();
        bitRate.setVariable(false);
        bitRate.setValue(BigInteger.valueOf(encoding.getBitRate()));
        return bitRate;
    }

    private UniqueIDType generateOtherId(Item item) {
        PublisherIdUtility config = configFactory.getIdUtil(item.getPublisher());
        UniqueIDType id = new UniqueIDType();
        id.setAuthority(config.getDeepLinkingAuthorityId());
        id.setValue(getAsin(item));
        return id;
    }

    private List<GenreType> generateGenres() {
        GenreType mediaAvailable = new GenreType();
        mediaAvailable.setType(GENRE_TYPE_OTHER);
        mediaAvailable.setHref(YOUVIEW_GENRE_MEDIA_AVAILABLE);
        
        GenreType subRequired = new GenreType();
        subRequired.setType(GENRE_TYPE_OTHER);
        subRequired.setHref(YOUVIEW_GENRE_SUBSCRIPTION_REQUIRED);
        
        return ImmutableList.of(mediaAvailable, subRequired);
    }

    private Duration generatePublishedDuration(Item item) {
        Version version = Iterables.getOnlyElement(item.getVersions());
        Integer durationInSecs = version.getDuration();
        if (durationInSecs != null) {
            return datatypeFactory.newDurationDayTime(durationInSecs * 1000);
        } 
        return null;
    }

    private XMLGregorianCalendar generateAvailabilityStart(Item item) {
        Version version = Iterables.getOnlyElement(item.getVersions());
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
        Policy policy = Iterables.getOnlyElement(encoding.getAvailableAt()).getPolicy();
        return datatypeFactory.newXMLGregorianCalendar(policy.getAvailabilityStart().toGregorianCalendar());
    }

    private XMLGregorianCalendar generateAvailabilityEnd(Item item) {
        Version version = Iterables.getOnlyElement(item.getVersions());
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
        Policy policy = Iterables.getOnlyElement(encoding.getAvailableAt()).getPolicy();
        return datatypeFactory.newXMLGregorianCalendar(policy.getAvailabilityEnd().toGregorianCalendar());
    }
}
