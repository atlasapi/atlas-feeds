package org.atlasapi.feeds.youview.amazon;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.youview.refdata.schemas._2011_07_06.ExtendedInstanceDescriptionType;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Quality;
import org.atlasapi.media.entity.Version;
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

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import static com.google.common.base.Preconditions.checkNotNull;

public class AmazonOnDemandLocationGenerator implements OnDemandLocationGenerator {

    private static final String ONDEMAND_SERVICE_ID = "http://amazon.com/services/on_demand/primevideo";
    public static final String DEEP_LINKING_AUTHORITY = "asin.amazon.com"; //requested to be a single ID (https://jira-ngyv.youview.co.uk/projects/ECOTEST/issues/ECOTEST-317?filter=allopenissues)
    public static final String ALL_ASINS_AUTHORITY = "ondemand.asin.amazon.com"; //requested to be a space separated list of all ASINS that contributed. (as above)

    private static final String YOUVIEW_MIX_TYPE = "urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3";
    public static final String YOUVIEW_ENTITLEMENT_SUBSCRIPTION = "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription";
    public static final String YOUVIEW_ENTITLEMENT_PAY_TO_RENT = "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#rental";
    public static final String YOUVIEW_ENTITLEMENT_PAY_TO_BUY = "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#ownership";
    public static final String YOUVIEW_GENRE_MEDIA_AVAILABLE = "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available";
    private static final String GENRE_TYPE_OTHER = "other";
    private static final Integer DEFAULT_BIT_RATE = 3200000;

    private final IdGenerator idGenerator;
    
    public AmazonOnDemandLocationGenerator(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    @Override
    public OnDemandProgramType generate(ItemOnDemandHierarchy onDemandHierarchy, String onDemandImi) {
        ExtendedOnDemandProgramType onDemand = new ExtendedOnDemandProgramType();
        List<Location> locations = onDemandHierarchy.locations();

        onDemand.setServiceIDRef(ONDEMAND_SERVICE_ID);
        onDemand.setProgram(generateProgram(onDemandHierarchy.item(), onDemandHierarchy.version()));
        onDemand.setInstanceMetadataId(onDemandImi);
        onDemand.setInstanceDescription(generateInstanceDescription(onDemandHierarchy));
        onDemand.setPublishedDuration(generatePublishedDuration(onDemandHierarchy.version()));
        //This assumes that all amazon locations represent the same thing, and thus have the same
        //start and end dates.
        onDemand.setStartOfAvailability(generateAvailabilityStart(locations.get(0)));
        //Amazon does not send start and end dates. These are fixed by us to certain dates.
        //YV has requested we do not send end-dates for content available indefinitely, and simply
        //revoke it if is no longer available.
       // onDemand.setEndOfAvailability(generateAvailabilityEnd(locations.get(0)));
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
        Encoding encoding = onDemandHie.encoding();
        ExtendedInstanceDescriptionType instanceDescription = new ExtendedInstanceDescriptionType();
        
        instanceDescription.getGenre().addAll(generateGenres(onDemandHie.locations()));
        instanceDescription.setAVAttributes(generateAvAttributes(encoding));
        instanceDescription.getOtherIdentifier().add(generateDeepLinkingId(onDemandHie.locations()));
        instanceDescription.getOtherIdentifier().add(generateOtherAuthorityId(onDemandHie.locations()));

        return instanceDescription;
    }

    private AVAttributesType generateAvAttributes(Encoding encoding) {
        AVAttributesType attributes = new AVAttributesType();

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

        //We no longer set hard-coded values during ingest, but YV requires something, so we'll hard-code them
        //here instead, and we'll match them to YV's SPECWIP-4212
        AspectRatioType aspectRatio = new AspectRatioType();
        if (Quality.SD.equals(encoding.getQuality())) {
            attributes.setHorizontalSize(848);
            attributes.setVerticalSize(480);
            aspectRatio.setValue("16:9");
        }
        if (Quality.HD.equals(encoding.getQuality())) {
            attributes.setHorizontalSize(1280);
            attributes.setVerticalSize(720); //lower value that is HD for YV.
            aspectRatio.setValue("16:9");
        }
        if (Quality.FOUR_K.equals(encoding.getQuality())) {
            attributes.setHorizontalSize(3840);
            attributes.setVerticalSize(2160); //minimum to considered UHD by YV
            aspectRatio.setValue("16:9");
        }


        if (encoding.getVideoAspectRatio() != null) {
            aspectRatio = new AspectRatioType();
            aspectRatio.setValue(encoding.getVideoAspectRatio());
            attributes.getAspectRatio().add(aspectRatio);
        }

        else {
            attributes.getAspectRatio().add(aspectRatio);
        }

        return attributes;
    }

    private BitRateType generateBitRate(Encoding encoding) {
        Integer bitRate = Objects.firstNonNull(encoding.getBitRate(), DEFAULT_BIT_RATE);
        BitRateType bitRateType = new BitRateType();
        bitRateType.setVariable(false);
        bitRateType.setValue(BigInteger.valueOf(bitRate));
        return bitRateType;
    }

    private UniqueIDType generateDeepLinkingId(List<Location> locations) {
        UniqueIDType id = new UniqueIDType();
        id.setAuthority(DEEP_LINKING_AUTHORITY);
        id.setValue(AmazonIdGenerator.getAsin(locations.get(0)));
        return id;
    }

    private UniqueIDType generateOtherAuthorityId(List<Location> locations) {
        UniqueIDType id = new UniqueIDType();
        id.setAuthority(ALL_ASINS_AUTHORITY);
        // YV requires us to send all contributing IDs separated by spaces (ECOTEST-317)
        StringBuilder contributing = new StringBuilder();
        Set<String> asins = new HashSet<>(); //put them set to remove duplicates.
        for (Location location : locations) {
            asins.add(AmazonIdGenerator.getAsin(location));
        }
        for (String asin : asins) {
            contributing.append(asin).append(" ");
        }
        //trim the last space.
        contributing = new StringBuilder(contributing.substring(0, contributing.length() - 1));
        id.setValue(contributing.toString());
        return id;
    }

    //Genres in this context describes the availability of the media. 
    private Set<GenreType> generateGenres(List<Location> locations) {

        List<GenreType> plans = new ArrayList<>();

        //All media ingested by Amazon is available
        GenreType mediaAvailable = new GenreType();
        mediaAvailable.setType(GENRE_TYPE_OTHER);
        mediaAvailable.setHref(YOUVIEW_GENRE_MEDIA_AVAILABLE);
        plans.add(mediaAvailable);
        //Order the list so that the final xml will be the same between runs
        locations = new ArrayList<>(locations);
        locations.sort(new LocationComparator());
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
            if(!setContainsGenre(plans, revenuePlan)) {
                plans.add(revenuePlan);
            }
        }
        return ImmutableSet.copyOf(plans);
    }

    private boolean setContainsGenre(List<GenreType> types, GenreType type) {
        for (GenreType genreType : types) {
            if (genreType.getType().equals(type.getType()) &&
                genreType.getHref().equals(type.getHref())) {
                return true;
            }
        }
        return false;
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

    public class LocationComparator implements Comparator<Location> {
        //the order is insignificant, we just want it to be consistent.
        private final Map<Policy.RevenueContract, Integer> scores = ImmutableMap.of(
                Policy.RevenueContract.PAY_TO_BUY, 1,
                Policy.RevenueContract.PAY_TO_RENT, 2,
                Policy.RevenueContract.SUBSCRIPTION, 3
        );

        @Override
        public int compare(Location l1, Location l2) {
            int l1score = scores.get(l1.getPolicy().getRevenueContract());
            int l2score = scores.get(l2.getPolicy().getRevenueContract());

           return l1score - l2score;
        }

        @Override
        public Comparator<Location> reversed() {
            return null;
        }

        @Override
        public Comparator<Location> thenComparing(Comparator<? super Location> other) {
            return null;
        }

        @Override
        public <U> Comparator<Location> thenComparing(
                Function<? super Location, ? extends U> keyExtractor,
                Comparator<? super U> keyComparator) {
            return null;
        }

        @Override
        public <U extends Comparable<? super U>> Comparator<Location> thenComparing(
                Function<? super Location, ? extends U> keyExtractor) {
            return null;
        }

        @Override
        public Comparator<Location> thenComparingInt(ToIntFunction<? super Location> keyExtractor) {
            return null;
        }

        @Override
        public Comparator<Location> thenComparingLong(ToLongFunction<? super Location> keyExtractor) {
            return null;
        }

        @Override
        public Comparator<Location> thenComparingDouble(
                ToDoubleFunction<? super Location> keyExtractor) {
            return null;
        }
    }

}
