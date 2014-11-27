package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.Duration;

import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.tvanytime.granular.GranularProgramInformationGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;

import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.DerivedFromType;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata._2010.TVAParentalGuidanceType;
import tva.metadata._2010.TVATimeType;
import tva.mpeg7._2008.ControlledTermUseType;
import tva.mpeg7._2008.UniqueIDType;

public final class NitroProgramInformationGenerator implements GranularProgramInformationGenerator {

    private static final String BBC_VERSION_PID_AUTHORITY = "vpid.bbc.co.uk";
    private static final Pattern BBC_VERSION_PID_URI_PATTERN = Pattern.compile("http://nitro.bbc.co.uk/programmes/(.*)");
    
    // TODO all this certificate code will likely change
    private static final String LANGUAGE = "en";
    private static final String YOUVIEW_UNRATED_PARENTAL_RATING = "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#unrated";
    private static final String YOUVIEW_WARNINGS_PARENTAL_RATING = "urn:dtg:metadata:cs:DTGContentWarningCS:2011:W";

    // TODO fix all certificate code
    private static final Predicate<Certificate> FILTER_CERT_FOR_GB = new Predicate<Certificate>() {
        @Override
        public boolean apply(Certificate input) {
            return input.country().equals(Countries.GB);
        }
    };

    private static final Predicate<Version> VERSION_WITH_RESTRICTION = new Predicate<Version>() {
        @Override
        public boolean apply(Version input) {
            return input.getRestriction() != null;
        }
    };
    
    private static final Function<Certificate, String> CERTIFICATE_TO_CLASSIFICATION = new Function<Certificate, String>() {
        @Override
        public String apply(Certificate input) {
            String href = YOUVIEW_CERTIFICATE_MAPPING.get(input.classification());
            if (href == null) {
                href = YOUVIEW_UNRATED_PARENTAL_RATING;
            }
            return href;
        }
    };
    
    // TODO are there other settings than this? E, NR, TBA etc
    private static final Map<String, String> YOUVIEW_CERTIFICATE_MAPPING = ImmutableMap.<String, String>builder()
            .put("U", "http://bbfc.org.uk/BBFCRatingCS/2002#U")
            .put("PG", "http://bbfc.org.uk/BBFCRatingCS/2002#PG")
            .put("12", "http://bbfc.org.uk/BBFCRatingCS/2002#12")
            .put("15", "http://bbfc.org.uk/BBFCRatingCS/2002#15")
            .put("18", "http://bbfc.org.uk/BBFCRatingCS/2002#18")
            .build();

    private static final Integer DEFAULT_DURATION = 30 * 60;
    
    private final IdGenerator idGenerator;
    
    public NitroProgramInformationGenerator(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    @Override
    public final ProgramInformationType generate(ItemAndVersion hierarchy, String versionCrid) {
        ProgramInformationType progInfo = new ProgramInformationType();

        progInfo.setProgramId(versionCrid);
        progInfo.setBasicDescription(generateBasicDescription(hierarchy.item(), hierarchy.version()));
        progInfo.setDerivedFrom(generateDerivedFromElem(hierarchy.item()));
        progInfo.setLang(LANGUAGE);
        
        Optional<UniqueIDType> bbcVersionPid = versionPidOtherId(hierarchy.version());
        if (bbcVersionPid.isPresent()) {
            progInfo.getOtherIdentifier().add(bbcVersionPid.get());
        }

        return progInfo;
    }
    
    private Optional<UniqueIDType> versionPidOtherId(Version version) {
        Matcher matcher = BBC_VERSION_PID_URI_PATTERN.matcher(version.getCanonicalUri());
        if (!matcher.find()) {
            return Optional.absent();
        }
        UniqueIDType id = new UniqueIDType();
        id.setAuthority(BBC_VERSION_PID_AUTHORITY);
        id.setValue(matcher.group());
        return Optional.of(id);
    }

    private BasicContentDescriptionType generateBasicDescription(Item item, Version version) {
        BasicContentDescriptionType basicDescription = new BasicContentDescriptionType();

        basicDescription.setParentalGuidance(generateParentalGuidance(item));
        Optional<TVATimeType> prodDate = generateProductionDate(item);
        if (prodDate.isPresent()) {
            basicDescription.setProductionDate(prodDate.get());
        }
        basicDescription.getProductionLocation().addAll(generateProductLocations(item));
        basicDescription.setDuration(generateDuration(version));

        return basicDescription;
    }

    private List<String> generateProductLocations(Item item) {
        return ImmutableList.copyOf(Iterables.transform(item.getCountriesOfOrigin(), new Function<Country, String>() {
            @Override
            public String apply(Country input) {
                return input.code().toLowerCase();
            }
        }));
    }

    private TVAParentalGuidanceType generateParentalGuidance(Item item) {
        Optional<Restriction> restriction = getRestrictionFor(item);
        if (!restriction.isPresent()) {
            return unratedParentalGuidance();
        }

        return withWarningParentalGuidance(restriction.get());
    }

    private TVAParentalGuidanceType unratedParentalGuidance() {
        TVAParentalGuidanceType parentalGuidance = new TVAParentalGuidanceType();
        ControlledTermUseType unratedUseType = new ControlledTermUseType();
        unratedUseType.setHref(YOUVIEW_UNRATED_PARENTAL_RATING);
        parentalGuidance.setParentalRating(unratedUseType);
        return parentalGuidance;
    }

    private TVAParentalGuidanceType withWarningParentalGuidance(Restriction restriction) {
        TVAParentalGuidanceType parentalGuidance = new TVAParentalGuidanceType();

        ControlledTermUseType useType = new ControlledTermUseType();
        useType.setHref(YOUVIEW_WARNINGS_PARENTAL_RATING);
        parentalGuidance.setParentalRating(useType);

        ExplanationType explanationType = new ExplanationType();
        explanationType.setLength(ExplanationLengthType.LONG);
        explanationType.setValue(restriction.getMessage());
        parentalGuidance.getExplanatoryText().add(explanationType);

        return parentalGuidance;
    }

    private Optional<Restriction> getRestrictionFor(Item item) {
        Optional<Version> version = Iterables.tryFind(item.getVersions(),
                VERSION_WITH_RESTRICTION);

        if (!version.isPresent()) {
            return Optional.absent();
        }

        return Optional.of(version.get().getRestriction());
    }

    private Duration generateDuration(Version version) {
        Integer durationInSecs = version.getDuration();
        if (durationInSecs == null) {
            durationInSecs = durationFromFirstBroadcast(version);
        } 
        return TvAnytimeElementFactory.durationFrom(org.joda.time.Duration.standardSeconds(durationInSecs));
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

    private Optional<TVATimeType> generateProductionDate(Item item) {
        if (item.getYear() != null) {
            TVATimeType productionDate = new TVATimeType();
            productionDate.setTimePoint(item.getYear().toString());
            return Optional.of(productionDate);
        }
        return Optional.absent();
    }
    
    private DerivedFromType generateDerivedFromElem(Item item) {
        DerivedFromType derivedFrom = new DerivedFromType();
        derivedFrom.setCrid(idGenerator.generateContentCrid(item));
        return derivedFrom;
    }
}
