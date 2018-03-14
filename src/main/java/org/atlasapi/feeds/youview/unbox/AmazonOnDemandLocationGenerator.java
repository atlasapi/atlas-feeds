package org.atlasapi.feeds.youview.unbox;

import java.math.BigInteger;
import java.util.List;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.youview.refdata.schemas._2011_07_06.ExtendedInstanceDescriptionType;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;
import org.jdom.IllegalDataException;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.YouViewGeneratorUtils.getAsin;

public class AmazonOnDemandLocationGenerator implements OnDemandLocationGenerator {

    private static final String UNBOX_ONDEMAND_SERVICE_ID = "http://amazon.com/services/on_demand/primevideo";
    public static final String UNBOX_DEEP_LINKING_ID = "deep_linking_id.amazon.com";
    
    private static final String YOUVIEW_MIX_TYPE = "urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3";
    public static final String YOUVIEW_ENTITLEMENT_SUBSCRIPTION = "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription";
    public static final String YOUVIEW_ENTITLEMENT_PAY_TO_RENT = "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#rental";
    public static final String YOUVIEW_ENTITLEMENT_PAY_TO_BUY = "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#ownership";
    public static final String YOUVIEW_GENRE_MEDIA_AVAILABLE = "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available";
    private static final String GENRE_TYPE_OTHER = "other";

    private final IdGenerator idGenerator;
    
    public AmazonOnDemandLocationGenerator(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    @Override
    public OnDemandProgramType generate(ItemOnDemandHierarchy onDemandHierarchy, String onDemandImi) {
        ExtendedOnDemandProgramType onDemand = new ExtendedOnDemandProgramType();
        List<Location> locations = onDemandHierarchy.locations();

        onDemand.setServiceIDRef(UNBOX_ONDEMAND_SERVICE_ID);
        onDemand.setProgram(generateProgram(onDemandHierarchy.item(), onDemandHierarchy.version()));
        onDemand.setInstanceMetadataId(onDemandImi);
        onDemand.setInstanceDescription(generateInstanceDescription(onDemandHierarchy));
        onDemand.setPublishedDuration(generatePublishedDuration(onDemandHierarchy.version()));
        //This assumes that all amazon locations represent the same thing, and thus have the same
        //start and end dates.
        onDemand.setStartOfAvailability(generateAvailabilityStart(locations.get(0)));
        onDemand.setEndOfAvailability(generateAvailabilityEnd(locations.get(0)));
        onDemand.setFree(generateFree());

        return onDemand;
    }
    
    // There is no such thing as a free meal.
    private FlagType generateFree() {
        FlagType free = new FlagType();
        free.setValue(false);
        return free;
    }

    private CRIDRefType generateProgram(Item item, Version version) {
        CRIDRefType program = new CRIDRefType();
        program.setCrid(idGenerator.generateVersionCrid(item, version));
        return program;
    }

    private InstanceDescriptionType generateInstanceDescription(ItemOnDemandHierarchy onDemandHie) {
        Item item = onDemandHie.item();
        Encoding encoding = onDemandHie.encoding();
        ExtendedInstanceDescriptionType instanceDescription = new ExtendedInstanceDescriptionType();
        
        instanceDescription.getGenre().addAll(generateGenres(onDemandHie.locations()));
        instanceDescription.setAVAttributes(generateAvAttributes(encoding));
        instanceDescription.getOtherIdentifier().add(generateOtherId(item));

        return instanceDescription;
    }

    private AVAttributesType generateAvAttributes(Encoding encoding) {
        AVAttributesType attributes = new AVAttributesType();

        attributes.getAudioAttributes().add(generateAudioAttributes());
        attributes.setVideoAttributes(generateVideoAttributes(encoding));
        Optional<BitRateType> bitRate = generateBitRate(encoding);
        if (bitRate.isPresent()) {
            attributes.setBitRate(bitRate.get());
        }
        
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
        if (encoding.getVideoAspectRatio() != null) {
            AspectRatioType aspectRatio = new AspectRatioType();
            aspectRatio.setValue(encoding.getVideoAspectRatio());
            attributes.getAspectRatio().add(aspectRatio);
        }

        return attributes;
    }

    private Optional<BitRateType> generateBitRate(Encoding encoding) {
        Integer bitRate = encoding.getBitRate();
        if (bitRate == null) {
            return Optional.absent();
        }
        BitRateType bitRateType = new BitRateType();
        bitRateType.setVariable(false);
        bitRateType.setValue(BigInteger.valueOf(bitRate));
        return Optional.of(bitRateType);
    }

    private UniqueIDType generateOtherId(Item item) {
        UniqueIDType id = new UniqueIDType();
        id.setAuthority(UNBOX_DEEP_LINKING_ID);
        id.setValue(getAsin(item));
        return id;
    }

    //Genres in this context describes the availability of the media. 
    private List<GenreType> generateGenres(List<Location> locations) {

        ImmutableList.Builder<GenreType> plans = ImmutableList.builder();

        //All media ingested by Amazon is available
        GenreType mediaAvailable = new GenreType();
        mediaAvailable.setType(GENRE_TYPE_OTHER);
        mediaAvailable.setHref(YOUVIEW_GENRE_MEDIA_AVAILABLE);
        plans.add(mediaAvailable);

        for (Location location : locations) {
            GenreType revenuePlan = new GenreType();
            revenuePlan.setType(GENRE_TYPE_OTHER);
            switch (location.getPolicy().getRevenueContract()) {
            case PAY_TO_BUY:
                revenuePlan.setHref(YOUVIEW_ENTITLEMENT_PAY_TO_BUY);
                break;
            case PAY_TO_RENT:
                revenuePlan.setHref(YOUVIEW_ENTITLEMENT_PAY_TO_RENT);
                break;
            case SUBSCRIPTION:
                revenuePlan.setHref(YOUVIEW_ENTITLEMENT_SUBSCRIPTION);
                break;
            default:
                throw new IllegalDataException(
                        "Amazon onDemand content is not accessible via sub, rent or buy. Location uri="
                        + location.getUri());
            }
            plans.add(revenuePlan);
        }
        return plans.build();
    }

    private Duration generatePublishedDuration(Version version) {
        Integer durationInSecs = version.getDuration();
        if (durationInSecs != null) {
            return TvAnytimeElementFactory.durationFrom(org.joda.time.Duration.standardSeconds(durationInSecs));
        } 
        return null;
    }

    private XMLGregorianCalendar generateAvailabilityStart(Location location) {
        Policy policy = location.getPolicy();
        return TvAnytimeElementFactory.gregorianCalendar(policy.getAvailabilityStart());
    }

    private XMLGregorianCalendar generateAvailabilityEnd(Location location) {
        Policy policy = location.getPolicy();
        return TvAnytimeElementFactory.gregorianCalendar(policy.getAvailabilityEnd());
    }
}
