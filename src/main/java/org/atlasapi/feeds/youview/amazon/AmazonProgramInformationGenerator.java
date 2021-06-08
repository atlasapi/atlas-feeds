package org.atlasapi.feeds.youview.amazon;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;
import com.youview.refdata.schemas._2011_07_06.ExtendedTargetingInformationType;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.DerivedFromType;
import tva.metadata._2010.ExplanationLengthType;
import tva.metadata._2010.ExplanationType;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata._2010.TVAParentalGuidanceType;
import tva.metadata._2010.TVATimeType;
import tva.metadata.extended._2010.ExtendedContentDescriptionType;
import tva.mpeg7._2008.ControlledTermUseType;
import tva.mpeg7._2008.UniqueIDType;

import javax.xml.datatype.Duration;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.YouViewGeneratorUtils.getAmazonAsin;


public class AmazonProgramInformationGenerator implements ProgramInformationGenerator {

    private static final Logger log = LoggerFactory.getLogger(AmazonProgramInformationGenerator.class);
    public static final String YOUVIEW_AMAZON_TARGET_USER_GROUP = "http://refdata.youview.com/mpeg7cs/YouViewApplicationPlayerCS/2017-09-28#html5-aiv1";
    
    private static final Predicate<Certificate> FILTER_CERT_FOR_GB = new Predicate<Certificate>() {
        @Override
        public boolean apply(Certificate input) {
            return input.country().equals(Countries.GB);
        }
    };
    
    private static final Function<Certificate, String> CERTIFICATE_TO_CLASSIFICATION = new Function<Certificate, String>() {
        @Override
        public String apply(Certificate input) {
            String href = YOUVIEW_CERTIFICATE_MAPPING.get(input.classification());
            if (href == null) {
                log.warn(
                        "Age rating classification from amazon ({}) could not be mapped to a YV certificate. This "
                        + "probably means that amazon started sending new classifications that "
                        + "are not in the existing map. Find this class in code, and update the map. "
                        + "In the meantime the default rating is used ({}).",
                        input.classification(), YOUVIEW_DEFAULT_CERTIFICATE);
                href = YOUVIEW_DEFAULT_CERTIFICATE;
            }
            return href;
        }
    };

    private static final Function<Certificate, String> CERTIFICATE_TO_EXPLANATION = new Function<Certificate, String>() {
        @Override
        public String apply(Certificate input) {
            String href = YOUVIEW_CERTIFICATE_EXPLANATION_MAPPING.get(input.classification());
            if (href == null) {
                href = YOUVIEW_DEFAULT_EXPLANATION;
            }
            return href;
        }
    };

    private static final String YOUVIEW_DEFAULT_CERTIFICATE = "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#unrated";
    private static final String YOUVIEW_DEFAULT_EXPLANATION = "Rating to be announced";
    // TODO: Add all classification systems
    private static final Map<String, String> YOUVIEW_CERTIFICATE_MAPPING = ImmutableMap.<String, String>builder()
            .put("amr|all", "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#no_parental_controls")
            .put("amr|7+", "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#twelve")
            .put("amr|13+", "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#fifteen")
            .put("amr|16+", "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#eighteen")
            .put("amr|18+", "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#eighteen")
            .put("amr|nr", "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#unrated")

            .put("bbfc|u", "http://bbfc.org.uk/BBFCRatingCS/2002#U")
            .put("bbfc|pg", "http://bbfc.org.uk/BBFCRatingCS/2002#PG")
            .put("bbfc|12", "http://bbfc.org.uk/BBFCRatingCS/2002#12")
            .put("bbfc|15", "http://bbfc.org.uk/BBFCRatingCS/2002#15")
            .put("bbfc|18", "http://bbfc.org.uk/BBFCRatingCS/2002#18")
            .put("bbfc|nr", "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#unrated")
            .put("bbfc|tbc", "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#unrated")
            .build();

    private static final Map<String, String> YOUVIEW_CERTIFICATE_EXPLANATION_MAPPING = ImmutableMap.<String, String>builder()
            .put("amr|all", "General audience")
            .put("amr|7+", "Suitable for 7 years or older")
            .put("amr|13+","Suitable for 13 years or older")
            .put("amr|16+","Suitable for 16 years or older")
            .put("amr|18+","Adults only")
            .put("amr|nr","Not rated")

            .put("bbfc|u", "Suitable for all")
            .put("bbfc|pg", "Parental guidance")
            .put("bbfc|12", "Video release suitable for 12 years and over")
            .put("bbfc|15", "Suitable only for 15 years and over")
            .put("bbfc|18", "Suitable only for adults")
            .put("bbfc|nr", "Not rated")
            .put("bbfc|tbc", "Not yet rated")
            .build();
    
    private final IdGenerator idGenerator;

    public AmazonProgramInformationGenerator(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    @Override
    public ProgramInformationType generate(ItemAndVersion versionHierarchy, String versionCrid) {
        ProgramInformationType progInfo = new ProgramInformationType();
        
        progInfo.setProgramId(versionCrid);
        progInfo.setBasicDescription(generateBasicDescription(versionHierarchy.item(), versionHierarchy.version()));
        progInfo.setDerivedFrom(generateDerivedFromElem(versionHierarchy.item()));

        return progInfo;
    }
    
    private DerivedFromType generateDerivedFromElem(Item item) {
        DerivedFromType derivedFrom = new DerivedFromType();
        derivedFrom.setCrid(idGenerator.generateContentCrid(item));
        return derivedFrom;
    }

    private UniqueIDType generateOtherId(Item item) {
        UniqueIDType id = new UniqueIDType();
        
        id.setAuthority(AmazonOnDemandLocationGenerator.DEEP_LINKING_AUTHORITY);
        id.setValue(getAmazonAsin(item));
        
        return id;
    }

    private BasicContentDescriptionType generateBasicDescription(Item item, Version version) {
        ExtendedContentDescriptionType basicDescription = new ExtendedContentDescriptionType();

        basicDescription.setParentalGuidance(generateParentalGuidance(item));
        Optional<TVATimeType> prodDate = generateProductionDate(item);
        if (prodDate.isPresent()) {
            basicDescription.setProductionDate(prodDate.get());
        }
        basicDescription.getProductionLocation().addAll(generateProductLocations(item));
        basicDescription.setDuration(generateDuration(version));
        // In order to ensure Amazon content is only discoverable on YouView devices which have
        // Amazon enabled, we need a Discovery User Group
        ControlledTermType targetUserGroup = new ControlledTermType();
        targetUserGroup.setHref(YOUVIEW_AMAZON_TARGET_USER_GROUP);
        ExtendedTargetingInformationType targetingInfo = new ExtendedTargetingInformationType();
        targetingInfo.getTargetUserGroup().add(targetUserGroup);
        basicDescription.getTargetingInformationOrTargetingInformationRef().add(targetingInfo);

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
        TVAParentalGuidanceType parentalGuidance = new TVAParentalGuidanceType();

        String certificate = Iterables.getFirst(
                item.getCertificates()
                        .stream()
                        .filter(FILTER_CERT_FOR_GB::apply)
                        .collect(Collectors.toList())
                        .stream()
                        .map(CERTIFICATE_TO_CLASSIFICATION::apply)
                        .collect(Collectors.toList()),
            YOUVIEW_DEFAULT_CERTIFICATE);

        ControlledTermUseType useType = new ControlledTermUseType();
        useType.setHref(certificate);
        parentalGuidance.setParentalRating(useType);

        String explanationText = Iterables.getFirst(
                item.getCertificates()
                        .stream()
                        .filter(FILTER_CERT_FOR_GB::apply)
                        .collect(Collectors.toList())
                        .stream()
                        .map(CERTIFICATE_TO_EXPLANATION::apply)
                        .collect(Collectors.toList()),
                "");

        ExplanationType explanationType = new ExplanationType();
        explanationType.setLength(ExplanationLengthType.LONG);
        explanationType.setValue(explanationText);
        parentalGuidance.getExplanatoryText().add(explanationType);
        return parentalGuidance;
    }

    private Duration generateDuration(Version version) {
        Integer durationInSecs = version.getDuration();
        if (durationInSecs != null) {
            return TvAnytimeElementFactory.durationFrom(org.joda.time.Duration.standardSeconds(durationInSecs));
        } 
        return null;
    }

    private Optional<TVATimeType> generateProductionDate(Item item) {
        if (item.getYear() == null) {
            return Optional.absent();
        }

        //YV's acceptable range
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, 365);
        int upperLimit = now.get(Calendar.YEAR);
        int lowerLimit = 1888;
        if(item.getYear() < lowerLimit || item.getYear() > upperLimit){
            return Optional.absent();
        }

        TVATimeType productionDate = new TVATimeType();
        productionDate.setTimePoint(item.getYear().toString());
        return Optional.of(productionDate);
    }
}