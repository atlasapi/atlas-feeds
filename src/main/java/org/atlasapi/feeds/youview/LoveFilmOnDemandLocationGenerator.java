package org.atlasapi.feeds.youview;

import static org.atlasapi.feeds.youview.LoveFilmOutputUtils.getId;

import java.math.BigInteger;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Policy;
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

public class LoveFilmOnDemandLocationGenerator implements OnDemandLocationGenerator {

    private static final String VERSION_SUFFIX = "_version";
    private static final String ASPECT_RATIO_16_9 = "16:9";
    private static final String YOUVIEW_MIX_TYPE = "urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3";
    private static final String DEEP_LINKING_ID_SUFFIX = "L";
    private static final String LOVEFILM_DEEP_LINKING_ID = "deep_linking_id.lovefilm.com";
    private static final String YOUVIEW_GENRE_SUBSCRIPTION_REQUIRED = "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription";
    private static final String YOUVIEW_GENRE_MEDIA_AVAILABLE = "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available";
    private static final String LOVEFILM_CRID_SEPARATOR = "_r";
    private static final String LOVEFILM_PRODUCT_CRID_PREFIX = "crid://lovefilm.com/product/";
    private static final String LOVEFILM_IDREF_ONDEMAND = "http://lovefilm.com/OnDemand";
    private static final String GENRE_TYPE_OTHER = "other";

    private DatatypeFactory datatypeFactory;

    /**
     * NB DatatypeFactory is required for creation of javax Durations
     * This DatatypeFactory class may not be threadsafe
     */
    public LoveFilmOnDemandLocationGenerator() {
        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            Throwables.propagate(e);
        }
    }
    
    @Override
    public OnDemandProgramType generate(Item item) {
        ExtendedOnDemandProgramType onDemand = new ExtendedOnDemandProgramType();
        
        onDemand.setServiceIDRef(LOVEFILM_IDREF_ONDEMAND);
        onDemand.setProgram(generateProgram(item));
        // TODO once again, this has a placeholder for the digital release id, which isn't currently ingested
        onDemand.setInstanceMetadataId("imi:lovefilm.com/t" + getId(item.getCanonicalUri()) + VERSION_SUFFIX);
        onDemand.setInstanceDescription(generateInstanceDescription(item));
        onDemand.setPublishedDuration(generatePublishedDuration(item));
        Optional<XMLGregorianCalendar> startOfAvailability = generateAvailabilityStart(item);
        if (startOfAvailability.isPresent()) {
            onDemand.setStartOfAvailability(startOfAvailability.get());
        }
        Optional<XMLGregorianCalendar> endOfAvailability = generateAvailabilityEnd(item);
        if (endOfAvailability.isPresent()) {
            onDemand.setEndOfAvailability(endOfAvailability.get());
        }
        onDemand.setFree(generateFree());

        return onDemand;
    }
    
    // hardcoded
    private FlagType generateFree() {
        FlagType free = new FlagType();
        free.setValue(false);
        return free;
    }

    private CRIDRefType generateProgram(Item item) {
        CRIDRefType program = new CRIDRefType();
        // TODO digital_release_id not ingested yet, currently a placeholder of id + '_version'
        program.setCrid(LOVEFILM_PRODUCT_CRID_PREFIX + getId(item.getCanonicalUri()) + VERSION_SUFFIX);
        return program;
    }

    private InstanceDescriptionType generateInstanceDescription(Item item) {
        InstanceDescriptionType instanceDescription = new InstanceDescriptionType();
        
        instanceDescription.getGenre().addAll(generateGenres());
        instanceDescription.setAVAttributes(generateAvAttributes());
        instanceDescription.getOtherIdentifier().add(generateOtherId(item));
        
        return instanceDescription;
    }

    private AVAttributesType generateAvAttributes() {
        AVAttributesType attributes = new AVAttributesType();
        
        attributes.getAudioAttributes().add(generateAudioAttributes());
        attributes.setVideoAttributes(generateVideoAttributes());
        attributes.setBitRate(generateBitRate());
        
        return attributes;
    }

    private AudioAttributesType generateAudioAttributes() {
        AudioAttributesType attributes = new AudioAttributesType();
        ControlledTermType mixType = new ControlledTermType();
        mixType.setHref(YOUVIEW_MIX_TYPE);
        attributes.setMixType(mixType);
        return attributes;
    }

    private VideoAttributesType generateVideoAttributes() {
        VideoAttributesType attributes = new VideoAttributesType();
        
        // TODO hardcoded, TBC by Lovefilm
        attributes.setHorizontalSize(1280);
        // TODO hardcoded, TBC by Lovefilm
        attributes.setVerticalSize(960);
        AspectRatioType aspectRatio = new AspectRatioType();
        aspectRatio.setValue(ASPECT_RATIO_16_9);
        attributes.getAspectRatio().add(aspectRatio);
        
        return attributes;
    }

    // TODO hardcoded values, TBC from Lovefilm
    private BitRateType generateBitRate() {
        BitRateType bitRate = new BitRateType();
        bitRate.setVariable(false);
        bitRate.setMinimum(BigInteger.valueOf(700));
        bitRate.setMaximum(BigInteger.valueOf(20000));
        bitRate.setAverage(BigInteger.valueOf(1350));
        bitRate.setValue(BigInteger.valueOf(900));
        return bitRate;
    }

    private UniqueIDType generateOtherId(Item item) {
        UniqueIDType id = new UniqueIDType();
        id.setAuthority(LOVEFILM_DEEP_LINKING_ID);
        id.setValue(getId(item.getCanonicalUri()) + DEEP_LINKING_ID_SUFFIX);
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

    private Optional<XMLGregorianCalendar> generateAvailabilityStart(Item item) {
        Version version = Iterables.getOnlyElement(item.getVersions());
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
        Policy policy = Iterables.getOnlyElement(encoding.getAvailableAt()).getPolicy();
        if (policy.getAvailabilityStart() != null) {
            return Optional.of(datatypeFactory.newXMLGregorianCalendar(policy.getAvailabilityStart().toGregorianCalendar()));
        }
        return Optional.absent();
    }

    private Optional<XMLGregorianCalendar> generateAvailabilityEnd(Item item) {
        Version version = Iterables.getOnlyElement(item.getVersions());
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
        Policy policy = Iterables.getOnlyElement(encoding.getAvailableAt()).getPolicy();
        if (policy.getAvailabilityEnd() != null) {
            return Optional.of(datatypeFactory.newXMLGregorianCalendar(policy.getAvailabilityEnd().toGregorianCalendar()));
        }
        return Optional.absent();
    }
}
