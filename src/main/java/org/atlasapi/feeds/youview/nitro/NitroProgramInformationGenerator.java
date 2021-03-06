package org.atlasapi.feeds.youview.nitro;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.Duration;

import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Version;

import com.metabroadcast.common.intl.Country;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.jdom.IllegalDataException;
import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.DerivedFromType;
import tva.metadata._2010.ExplanationLengthType;
import tva.metadata._2010.ExplanationType;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata._2010.TVAParentalGuidanceType;
import tva.metadata._2010.TVATimeType;
import tva.mpeg7._2008.ControlledTermUseType;
import tva.mpeg7._2008.UniqueIDType;

import static com.google.common.base.Preconditions.checkNotNull;

public final class NitroProgramInformationGenerator implements ProgramInformationGenerator {

    private static final String BBC_VERSION_PID_AUTHORITY = "vpid.bbc.co.uk";
    private static final Pattern BBC_VERSION_PID_URI_PATTERN = Pattern.compile("http://nitro.bbc.co.uk/programmes/(.*)");
    
    private static final String LANGUAGE = "en";
    private static final String YOUVIEW_UNRATED_PARENTAL_RATING = "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#unrated";
    private static final String YOUVIEW_WARNINGS_PARENTAL_RATING = "urn:dtg:metadata:cs:DTGContentWarningCS:2011:W";
    
    private static final Predicate<Version> VERSION_WITH_RESTRICTION = new Predicate<Version>() {
        @Override
        public boolean apply(Version input) {
            Restriction restriction = input.getRestriction();
            return restriction != null && Boolean.TRUE.equals(restriction.isRestricted());
        }
    };
    
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

        basicDescription.setParentalGuidance(generateParentalGuidance(version));
        Optional<TVATimeType> prodDate = generateProductionDate(item);
        if (prodDate.isPresent()) {
            basicDescription.setProductionDate(prodDate.get());
        }
        basicDescription.getProductionLocation().addAll(generateProductLocations(item));
        Duration versionDuration = generateDuration(version);
        // YouView *requires* durations on versions, so throw up if we don't have one
        if (versionDuration == null) {
            throw new IllegalDataException("null version duration for item " + item.getCanonicalUri());
        }
        basicDescription.setDuration(versionDuration);

        return basicDescription;
    }

    private List<String> generateProductLocations(Item item) {
        return ImmutableList.copyOf(Iterables.transform(item.getCountriesOfOrigin(),
                new Function<Country, String>() {

                    @Override
                    public String apply(Country input) {
                        return input.code().toLowerCase();
                    }
                }));
    }

    private TVAParentalGuidanceType generateParentalGuidance(Version version) {
        if (!VERSION_WITH_RESTRICTION.apply(version)) {
            return unratedParentalGuidance();
        }

        return withWarningParentalGuidance(version.getRestriction());
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

    private Duration generateDuration(Version version) {
        Integer durationInSecs = version.getDuration();
        if (durationInSecs == null) {
            return null;
        } 
        return TvAnytimeElementFactory.durationFrom(org.joda.time.Duration.standardSeconds(durationInSecs));
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
