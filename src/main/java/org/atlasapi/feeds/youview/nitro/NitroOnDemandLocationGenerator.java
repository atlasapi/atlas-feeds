package org.atlasapi.feeds.youview.nitro;

import java.math.BigInteger;
import java.util.List;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.AbstractOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;

import tva.metadata._2010.AVAttributesType;
import tva.metadata._2010.AspectRatioType;
import tva.metadata._2010.AudioAttributesType;
import tva.metadata._2010.AudioLanguageType;
import tva.metadata._2010.BitRateType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.FlagType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.SignLanguageType;
import tva.metadata._2010.VideoAttributesType;

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
    private static final String AUDIO_DESCRIPTION_PURPOSE = "urn:tva:metadata:cs:AudioPurposeCS:2007:1";
    private static final String AUDIO_DESCRIPTION_TYPE = "dubbed";
    private static final String ENGLISH_LANG = "en";
    private static final String BRITISH_SIGN_LANGUAGE = "bfi";

    private final TvAnytimeElementFactory elementFactory = TvAnytimeElementFactory.INSTANCE;

    public NitroOnDemandLocationGenerator(IdGenerator idGenerator, OnDemandHierarchyExpander hierarchyExpander) {
        super(idGenerator, hierarchyExpander);
    }
    
    @Override
    public final OnDemandProgramType generate(String imi, Item item, Version version, Encoding encoding, Location location) {
        
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

        if (encoding.getSigned()) {
            SignLanguageType signLanguageType = new SignLanguageType();
            signLanguageType.setValue(BRITISH_SIGN_LANGUAGE);
            instanceDescription.getSignLanguage().add(signLanguageType);
        }
        
        return instanceDescription;
    }

    private AVAttributesType generateAvAttributes(Encoding encoding) {
        AVAttributesType attributes = new AVAttributesType();
        attributes.getAudioAttributes().add(generateAudioAttributes(audioDescribedOrFalse(encoding)));
        attributes.setVideoAttributes(generateVideoAttributes(encoding));
        attributes.setBitRate(generateBitRate(encoding));

        return attributes;
    }

    private boolean audioDescribedOrFalse(Encoding encoding) {
        return Optional.fromNullable(encoding.getAudioDescribed()).or(false);
    }

    private AudioAttributesType generateAudioAttributes(boolean audioDescribed) {
        AudioAttributesType attributes = new AudioAttributesType();
        ControlledTermType mixType = new ControlledTermType();
        mixType.setHref(MIX_TYPE_STEREO);
        attributes.setMixType(mixType);

        if (audioDescribed) {
            AudioLanguageType audioLanguage = new AudioLanguageType();
            audioLanguage.setSupplemental(true);
            audioLanguage.setType(AUDIO_DESCRIPTION_TYPE);
            audioLanguage.setPurpose(AUDIO_DESCRIPTION_PURPOSE);
            audioLanguage.setValue(ENGLISH_LANG);
            attributes.setAudioLanguage(audioLanguage);
        }
        
        return attributes;
    }
    
    private VideoAttributesType generateVideoAttributes(Encoding encoding) {
        VideoAttributesType videoAttributes = new VideoAttributesType();

        videoAttributes.setHorizontalSize(Optional.fromNullable(encoding.getVideoHorizontalSize())
                .or(DEFAULT_HORIZONTAL_SIZE));
        videoAttributes.setVerticalSize(Optional.fromNullable(encoding.getVideoVerticalSize())
                .or(DEFAULT_VERTICAL_SIZE));
        
        AspectRatioType aspectRatio = new AspectRatioType();
        aspectRatio.setValue(Objects.firstNonNull(encoding.getVideoAspectRatio(), ASPECT_RATIO));
        
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
        return elementFactory.durationFrom(org.joda.time.Duration.standardSeconds(durationInSecs));
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
